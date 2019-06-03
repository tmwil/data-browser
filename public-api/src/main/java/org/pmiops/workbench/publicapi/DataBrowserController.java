package org.pmiops.workbench.publicapi;

import java.util.logging.Logger;
import org.apache.commons.lang3.math.NumberUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.TreeSet;
import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cdr.dao.QuestionConceptDao;
import org.pmiops.workbench.cdr.dao.AchillesAnalysisDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDao;
import org.pmiops.workbench.cdr.dao.AchillesResultDistDao;
import org.pmiops.workbench.cdr.model.SurveyQuestionMap;
import org.pmiops.workbench.model.SurveyQuestionAnalysis;
import org.pmiops.workbench.model.SurveyQuestionResult;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.model.AchillesResult;
import org.pmiops.workbench.cdr.model.AchillesAnalysis;
import org.pmiops.workbench.cdr.model.AchillesResultDist;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.pmiops.workbench.cdr.model.QuestionConcept;
import org.pmiops.workbench.cdr.model.SurveyModule;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.ConceptAnalysis;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.MatchType;
import org.pmiops.workbench.model.QuestionConceptListResponse;
import org.pmiops.workbench.model.ConceptAnalysisListResponse;
import org.pmiops.workbench.model.CriteriaParentResponse;
import org.pmiops.workbench.model.CriteriaListResponse;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.DomainInfosAndSurveyModulesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@RestController
public class DataBrowserController implements DataBrowserApiDelegate {

    @Autowired
    private ConceptDao conceptDao;
    @Autowired
    private CdrVersionDao cdrVersionDao;
    @Autowired
    private CriteriaDao criteriaDao;
    @Autowired
    private QuestionConceptDao  questionConceptDao;
    @Autowired
    private AchillesAnalysisDao achillesAnalysisDao;
    @Autowired
    private AchillesResultDao achillesResultDao;
    @Autowired
    private DomainInfoDao domainInfoDao;
    @Autowired
    private SurveyModuleDao surveyModuleDao;
    @Autowired
    private AchillesResultDistDao achillesResultDistDao;
    @PersistenceContext(unitName = "cdr")
    private EntityManager entityManager;
    @Autowired
    @Qualifier("defaultCdr")
    private Provider<CdrVersion> defaultCdrVersionProvider;
    @Autowired
    private ConceptService conceptService;

    private static final Logger logger = Logger.getLogger(DataBrowserController.class.getName());

    public static final long PARTICIPANT_COUNT_ANALYSIS_ID = 1;
    public static final long COUNT_ANALYSIS_ID = 3000;
    public static final long GENDER_ANALYSIS_ID = 3101;
    public static final long GENDER_IDENTITY_ANALYSIS_ID = 3107;
    public static final long RACE_ETHNICITY_ANALYSIS_ID = 3108;
    public static final long AGE_ANALYSIS_ID = 3102;

    public static final long RACE_ANALYSIS_ID = 3103;
    public static final long ETHNICITY_ANALYSIS_ID = 3104;

    public static final long MEASUREMENT_DIST_ANALYSIS_ID = 1815;

    public static final long MEASUREMENT_GENDER_DIST_ANALYSIS_ID = 1815;

    public static final long MEASUREMENT_GENDER_ANALYSIS_ID = 1900;
    public static final long MEASUREMENT_GENDER_UNIT_ANALYSIS_ID = 1910;

    public static final long MALE = 8507;
    public static final long FEMALE = 8532;
    public static final long INTERSEX = 1585848;
    public static final long NONE = 1585849;
    public static final long OTHER = 0;

    public static final long GENDER_ANALYSIS = 2;
    public static final long RACE_ANALYSIS = 4;
    public static final long ETHNICITY_ANALYSIS = 5;

    public DataBrowserController() {}

    public DataBrowserController(ConceptService conceptService, ConceptDao conceptDao, CriteriaDao criteriaDao,
                                 DomainInfoDao domainInfoDao, SurveyModuleDao surveyModuleDao,
                                 AchillesResultDao achillesResultDao,
                                 AchillesAnalysisDao achillesAnalysisDao, AchillesResultDistDao achillesResultDistDao,
                                 EntityManager entityManager, Provider<CdrVersion> defaultCdrVersionProvider,
                                 CdrVersionDao cdrVersionDao) {
        this.conceptService = conceptService;
        this.conceptDao = conceptDao;
        this.criteriaDao = criteriaDao;
        this.domainInfoDao = domainInfoDao;
        this.surveyModuleDao = surveyModuleDao;
        this.achillesResultDao = achillesResultDao;
        this.achillesAnalysisDao = achillesAnalysisDao;
        this.achillesResultDistDao = achillesResultDistDao;
        this.entityManager = entityManager;
        this.defaultCdrVersionProvider = defaultCdrVersionProvider;
        this.cdrVersionDao = cdrVersionDao;
    }

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<Concept, org.pmiops.workbench.model.Concept>
            TO_CLIENT_CONCEPT =
            new Function<Concept, org.pmiops.workbench.model.Concept>() {
                @Override
                public org.pmiops.workbench.model.Concept apply(Concept concept) {
                    return new org.pmiops.workbench.model.Concept()
                            .conceptId(concept.getConceptId())
                            .conceptName(concept.getConceptName())
                            .standardConcept(concept.getStandardConcept())
                            .conceptCode(concept.getConceptCode())
                            .conceptClassId(concept.getConceptClassId())
                            .vocabularyId(concept.getVocabularyId())
                            .domainId(concept.getDomainId())
                            .countValue(concept.getCountValue())
                            .sourceCountValue(concept.getSourceCountValue())
                            .prevalence(concept.getPrevalence())
                            .conceptSynonyms(concept.getSynonyms());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<QuestionConcept, org.pmiops.workbench.model.QuestionConcept>
            TO_CLIENT_QUESTION_CONCEPT =
            new Function<QuestionConcept, org.pmiops.workbench.model.QuestionConcept>() {
                @Override
                public org.pmiops.workbench.model.QuestionConcept apply(QuestionConcept concept) {
                    SurveyQuestionAnalysis countAnalysis=null;
                    SurveyQuestionAnalysis genderAnalysis=null;
                    SurveyQuestionAnalysis ageAnalysis=null;
                    SurveyQuestionAnalysis genderIdentityAnalysis=null;
                    SurveyQuestionAnalysis raceEthnicityAnalysis=null;
                    if(concept.getCountAnalysis() != null){
                        countAnalysis = TO_CLIENT_SURVEY_ANALYSIS.apply(concept.getCountAnalysis());
                    }
                    if(concept.getGenderAnalysis() != null){
                        genderAnalysis = TO_CLIENT_SURVEY_ANALYSIS.apply(concept.getGenderAnalysis());
                    }
                    if(concept.getAgeAnalysis() != null){
                        ageAnalysis = TO_CLIENT_SURVEY_ANALYSIS.apply(concept.getAgeAnalysis());
                    }
                    if(concept.getGenderIdentityAnalysis() != null){
                        genderIdentityAnalysis = TO_CLIENT_SURVEY_ANALYSIS.apply(concept.getGenderIdentityAnalysis());
                    }
                    if(concept.getRaceEthnicityAnalysis() != null){
                        raceEthnicityAnalysis = TO_CLIENT_SURVEY_ANALYSIS.apply(concept.getRaceEthnicityAnalysis());
                    }


                    return new org.pmiops.workbench.model.QuestionConcept()
                            .conceptId(concept.getConceptId())
                            .conceptName(concept.getConceptName())
                            .conceptCode(concept.getConceptCode())
                            .domainId(concept.getDomainId())
                            .countValue(concept.getCountValue())
                            .prevalence(concept.getPrevalence())
                            .questions(concept.getQuestions().stream().map(TO_CLIENT_SURVEY_QUESTION_MAP).collect(Collectors.toList()))
                            .countAnalysis(countAnalysis)
                            .genderAnalysis(genderAnalysis)
                            .ageAnalysis(ageAnalysis)
                            .genderIdentityAnalysis(genderIdentityAnalysis)
                            .raceEthnicityAnalysis(raceEthnicityAnalysis);

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<Criteria, org.pmiops.workbench.model.Criteria>
            TO_CLIENT_CRITERIA =
            new Function<Criteria, org.pmiops.workbench.model.Criteria>() {
                @Override
                public org.pmiops.workbench.model.Criteria apply(Criteria criteria) {
                    return new org.pmiops.workbench.model.Criteria()
                            .id(criteria.getId())
                            .parentId(criteria.getParentId())
                            .type(criteria.getType())
                            .subtype(criteria.getType())
                            .code(criteria.getCode())
                            .name(criteria.getName())
                            .group(criteria.getGroup())
                            .selectable(criteria.getSelectable())
                            .count(Long.valueOf(criteria.getCount()))
                            .domainId(criteria.getDomainId())
                            .conceptId(criteria.getConceptId())
                            .path(criteria.getPath());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesAnalysis, org.pmiops.workbench.model.Analysis>
            TO_CLIENT_ANALYSIS =
            new Function<AchillesAnalysis, org.pmiops.workbench.model.Analysis>() {
                @Override
                public org.pmiops.workbench.model.Analysis apply(AchillesAnalysis cdr) {
                    List<org.pmiops.workbench.model.AchillesResult> results = new ArrayList<>();
                    if (!cdr.getResults().isEmpty()) {
                        results = cdr.getResults().stream().map(TO_CLIENT_ACHILLES_RESULT).collect(Collectors.toList());
                    }

                    List<org.pmiops.workbench.model.AchillesResultDist> distResults = new ArrayList<>();
                    if (!cdr.getDistResults().isEmpty()) {
                        distResults = cdr.getDistResults().stream().map(TO_CLIENT_ACHILLES_RESULT_DIST).collect(Collectors.toList());
                    }

                    return new org.pmiops.workbench.model.Analysis()
                            .analysisId(cdr.getAnalysisId())
                            .analysisName(cdr.getAnalysisName())
                            .stratum1Name(cdr.getStratum1Name())
                            .stratum2Name(cdr.getStratum2Name())
                            .stratum3Name(cdr.getStratum3Name())
                            .stratum4Name(cdr.getStratum4Name())
                            .stratum5Name(cdr.getStratum5Name())
                            .chartType(cdr.getChartType())
                            .dataType(cdr.getDataType())
                            .unitName(cdr.getUnitName())
                            .results(results)
                            .distResults(distResults)
                            .unitName(cdr.getUnitName());

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesAnalysis, SurveyQuestionAnalysis>
            TO_CLIENT_SURVEY_ANALYSIS =
            new Function<AchillesAnalysis, SurveyQuestionAnalysis>() {
                @Override
                public SurveyQuestionAnalysis apply(AchillesAnalysis cdr) {
                    List<SurveyQuestionResult> results = new ArrayList<>();
                    if (!cdr.getResults().isEmpty()) {
                        results = cdr.getResults().stream().map(TO_CLIENT_SURVEY_RESULT).collect(Collectors.toList());
                    }

                    return new SurveyQuestionAnalysis()
                            .analysisId(cdr.getAnalysisId())
                            .analysisName(cdr.getAnalysisName())
                            .stratum1Name(cdr.getStratum1Name())
                            .stratum2Name(cdr.getStratum2Name())
                            .stratum3Name(cdr.getStratum3Name())
                            .stratum4Name(cdr.getStratum4Name())
                            .stratum5Name(cdr.getStratum5Name())
                            .chartType(cdr.getChartType())
                            .dataType(cdr.getDataType())
                            .surveyQuestionResults(results);

                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<ConceptAnalysis, ConceptAnalysis>
            TO_CLIENT_CONCEPTANALYSIS=
            new Function<ConceptAnalysis, ConceptAnalysis>() {
                @Override
                public ConceptAnalysis apply(ConceptAnalysis ca) {
                    return new ConceptAnalysis()
                            .conceptId(ca.getConceptId())
                            .countAnalysis(ca.getCountAnalysis())
                            .genderAnalysis(ca.getGenderAnalysis())
                            .genderIdentityAnalysis(ca.getGenderIdentityAnalysis())
                            .raceEthnicityAnalysis(ca.getRaceEthnicityAnalysis())
                            .ageAnalysis(ca.getAgeAnalysis())
                            .raceAnalysis(ca.getRaceAnalysis())
                            .ethnicityAnalysis(ca.getEthnicityAnalysis())
                            .measurementValueGenderAnalysis(ca.getMeasurementValueGenderAnalysis())
                            .measurementValueAgeAnalysis(ca.getMeasurementValueAgeAnalysis())
                            .measurementDistributionAnalysis(ca.getMeasurementDistributionAnalysis())
                            .measurementGenderCountAnalysis(ca.getMeasurementGenderCountAnalysis());
                }
            };


    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesResult, org.pmiops.workbench.model.AchillesResult>
            TO_CLIENT_ACHILLES_RESULT =
            new Function<AchillesResult, org.pmiops.workbench.model.AchillesResult>() {
                @Override
                public org.pmiops.workbench.model.AchillesResult apply(AchillesResult o) {

                    return new org.pmiops.workbench.model.AchillesResult()
                            .id(o.getId())
                            .analysisId(o.getAnalysisId())
                            .stratum1(o.getStratum1())
                            .stratum2(o.getStratum2())
                            .stratum3(o.getStratum3())
                            .stratum4(o.getStratum4())
                            .stratum5(o.getStratum5())
                            .analysisStratumName(o.getAnalysisStratumName())
                            .countValue(o.getCountValue())
                            .sourceCountValue(o.getSourceCountValue());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesResult, SurveyQuestionResult>
            TO_CLIENT_SURVEY_RESULT =
            new Function<AchillesResult, SurveyQuestionResult>() {
                @Override
                public SurveyQuestionResult apply(AchillesResult o) {

                    return new SurveyQuestionResult()
                            .id(o.getId())
                            .analysisId(o.getAnalysisId())
                            .stratum1(o.getStratum1())
                            .stratum2(o.getStratum2())
                            .stratum3(o.getStratum3())
                            .stratum4(o.getStratum4())
                            .stratum5(o.getStratum5())
                            .analysisStratumName(o.getAnalysisStratumName())
                            .countValue(o.getCountValue())
                            .sourceCountValue(o.getSourceCountValue())
                            .subQuestions(null);
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<SurveyQuestionMap, org.pmiops.workbench.model.SurveyQuestionMap>
            TO_CLIENT_SURVEY_QUESTION_MAP =
            new Function<SurveyQuestionMap, org.pmiops.workbench.model.SurveyQuestionMap>() {
                @Override
                public org.pmiops.workbench.model.SurveyQuestionMap apply(SurveyQuestionMap sqm) {
                    return new org.pmiops.workbench.model.SurveyQuestionMap()
                            .id(sqm.getId())
                            .surveyConceptId(sqm.getSurveyConceptId())
                            .questionConceptId(sqm.getQuestionConceptId())
                            .surveyOrderNumber(sqm.getSurveyOrderNumber())
                            .questionOrderNumber(sqm.getQuestionOrderNumber())
                            .path(sqm.getPath())
                            .sub(sqm.getSub());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<CdrVersion, org.pmiops.workbench.model.CdrVersion>
            TO_CLIENT_CDR_VERSION =
            new Function<CdrVersion, org.pmiops.workbench.model.CdrVersion>() {
                @Override
                public org.pmiops.workbench.model.CdrVersion apply(CdrVersion cdrVersion) {
                    return new org.pmiops.workbench.model.CdrVersion()
                            .name(cdrVersion.getName())
                            .cdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()))
                            .numParticipants(cdrVersion.getNumParticipants())
                            .creationTime(cdrVersion.getCreationTime().getTime());
                }
            };

    /**
     * Converter function from backend representation (used with Hibernate) to
     * client representation (generated by Swagger).
     */
    private static final Function<AchillesResultDist, org.pmiops.workbench.model.AchillesResultDist>
            TO_CLIENT_ACHILLES_RESULT_DIST =
            new Function<AchillesResultDist, org.pmiops.workbench.model.AchillesResultDist>() {
                @Override
                public org.pmiops.workbench.model.AchillesResultDist apply(AchillesResultDist o) {

                    return new org.pmiops.workbench.model.AchillesResultDist()
                            .id(o.getId())
                            .analysisId(o.getAnalysisId())
                            .stratum1(o.getStratum1())
                            .stratum2(o.getStratum2())
                            .stratum3(o.getStratum3())
                            .stratum4(o.getStratum4())
                            .stratum5(o.getStratum5())
                            .countValue(o.getCountValue())
                            .minValue(o.getMinValue())
                            .maxValue(o.getMaxValue())
                            .avgValue(o.getAvgValue())
                            .stdevValue(o.getStdevValue())
                            .medianValue(o.getMedianValue())
                            .p10Value(o.getP10Value())
                            .p25Value(o.getP25Value())
                            .p75Value(o.getP75Value())
                            .p90Value(o.getP90Value());
                }
            };

    @Override
    public ResponseEntity<CriteriaParentResponse> getCriteriaRolledCounts(Long conceptId) {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        List<Criteria> criteriaList = criteriaDao.findParentCounts(String.valueOf(conceptId));
        CriteriaParentResponse response = new CriteriaParentResponse();
        if (criteriaList.size() > 0) {
            Criteria parent = criteriaList.get(0);
            if (criteriaList.size() >= 1) {
                criteriaList.remove(parent);
            }
            response.setParent(TO_CLIENT_CRITERIA.apply(parent));
            Multimap<Long, Criteria> parentCriteria = Multimaps
                    .index(criteriaList, Criteria::getParentId);
            CriteriaListResponse criteriaListResponse = new CriteriaListResponse();
            criteriaListResponse.setItems(parentCriteria.get(parent.getParentId()).stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));
            response.setChildren(criteriaListResponse);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CriteriaListResponse> getCriteriaChildren(Long parentId) {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        List<Criteria> criteriaList = criteriaDao.findCriteriaChildren(parentId);
        CriteriaListResponse criteriaListResponse = new CriteriaListResponse();
        criteriaListResponse.setItems(criteriaList.stream().map(TO_CLIENT_CRITERIA).collect(Collectors.toList()));
        return ResponseEntity.ok(criteriaListResponse);
    }

    @Override
    public ResponseEntity<DomainInfosAndSurveyModulesResponse> getDomainSearchResults(String query){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        String domainKeyword = ConceptService.modifyMultipleMatchKeyword(query, ConceptService.SearchType.DOMAIN_COUNTS);
        String surveyKeyword = ConceptService.modifyMultipleMatchKeyword(query, ConceptService.SearchType.SURVEY_COUNTS);
        Long conceptId = 0L;
        try {
            conceptId = Long.parseLong(query);
        } catch (NumberFormatException e) {
            // expected
        }
        // TODO: consider parallelizing these lookups
        List<Long> toMatchConceptIds = new ArrayList<>();
        toMatchConceptIds.add(conceptId);
        List<Concept> drugMatchedConcepts = conceptDao.findDrugIngredientsByBrand(query);
        if (drugMatchedConcepts.size() > 0) {
            toMatchConceptIds.addAll(drugMatchedConcepts.stream().map(Concept::getConceptId).collect(Collectors.toList()));
        }

        List<DomainInfo> domains = domainInfoDao.findStandardOrCodeMatchConceptCounts(domainKeyword, query, toMatchConceptIds);
        List<SurveyModule> surveyModules = surveyModuleDao.findSurveyModuleQuestionCounts(surveyKeyword);
        DomainInfosAndSurveyModulesResponse response = new DomainInfosAndSurveyModulesResponse();
        response.setDomainInfos(domains.stream()
                .map(DomainInfo.TO_CLIENT_DOMAIN_INFO)
                .collect(Collectors.toList()));
        response.setSurveyModules(surveyModules.stream()
                .map(SurveyModule.TO_CLIENT_SURVEY_MODULE)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ConceptListResponse> searchConcepts(SearchConceptsRequest searchConceptsRequest){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        Integer maxResults = searchConceptsRequest.getMaxResults();
        if(maxResults == null || maxResults == 0){
            maxResults = Integer.MAX_VALUE;
        }

        Integer minCount = searchConceptsRequest.getMinCount();
        if(minCount == null){
            minCount = 1;
        }

        StandardConceptFilter standardConceptFilter = searchConceptsRequest.getStandardConceptFilter();


        if(searchConceptsRequest.getQuery() == null || searchConceptsRequest.getQuery().isEmpty()){
            if(standardConceptFilter == null || standardConceptFilter == StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH){
                standardConceptFilter = StandardConceptFilter.STANDARD_CONCEPTS;
            }
        }else{
            if(standardConceptFilter == null){
                standardConceptFilter = StandardConceptFilter.STANDARD_OR_CODE_ID_MATCH;
            }
        }

        String domainId = null;
        if (searchConceptsRequest.getDomain() != null) {
            domainId = CommonStorageEnums.domainToDomainId(searchConceptsRequest.getDomain());
        }

        ConceptService.StandardConceptFilter convertedConceptFilter = ConceptService.StandardConceptFilter.valueOf(standardConceptFilter.name());

        Slice<Concept> concepts = null;
        concepts = conceptService.searchConcepts(searchConceptsRequest.getQuery(), convertedConceptFilter,
                searchConceptsRequest.getVocabularyIds(), domainId, maxResults, minCount);
        ConceptListResponse response = new ConceptListResponse();

        for(Concept con : concepts.getContent()){
            String conceptCode = con.getConceptCode();
            String conceptId = String.valueOf(con.getConceptId());

            if((con.getStandardConcept() == null || !con.getStandardConcept().equals("S") ) && (searchConceptsRequest.getQuery().equals(conceptCode) || searchConceptsRequest.getQuery().equals(conceptId))){
                List<Concept> stdConcepts = conceptDao.findStandardConcepts(con.getConceptId());
                response.setStandardConcepts(stdConcepts.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
                response.setSourceOfStandardConcepts(con.getConceptId());
            }

            if(!Strings.isNullOrEmpty(searchConceptsRequest.getQuery()) && (searchConceptsRequest.getQuery().equals(conceptCode) || searchConceptsRequest.getQuery().equals(conceptId))) {
                response.setMatchType(conceptCode.equals(searchConceptsRequest.getQuery()) ? MatchType.CODE : MatchType.ID );
                response.setMatchedConceptName(con.getConceptName());
            }
        }

        if(response.getMatchType() == null && response.getStandardConcepts() == null){
            response.setMatchType(MatchType.NAME);
        }

        List<Concept> conceptList = new ArrayList<>();

        if (concepts != null) {
            conceptList = new ArrayList(concepts.getContent());
            if(response.getStandardConcepts() != null) {
                conceptList = conceptList.stream().filter(c -> Long.valueOf(c.getConceptId()) != Long.valueOf(response.getSourceOfStandardConcepts())).collect(Collectors.toList());
            }
        }

        if(searchConceptsRequest.getDomain() != null && searchConceptsRequest.getDomain().equals(Domain.DRUG) && !searchConceptsRequest.getQuery().isEmpty()) {
            List<Concept> drugMatchedConcepts = new ArrayList<>();
            if (conceptList.size() > 0) {
                drugMatchedConcepts = conceptDao.findDrugIngredientsByBrandNotInConceptIds(searchConceptsRequest.getQuery(), conceptList.stream().map(Concept::getConceptId).collect(Collectors.toList()));
            } else {
                drugMatchedConcepts = conceptDao.findDrugIngredientsByBrand(searchConceptsRequest.getQuery());
            }
            if(drugMatchedConcepts.size() > 0) {
                conceptList.addAll(drugMatchedConcepts);
            }
        }

        response.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DomainInfosAndSurveyModulesResponse> getDomainTotals(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        List<DomainInfo> domainInfos = ImmutableList.copyOf(domainInfoDao.findByOrderByDomainId());
        List<SurveyModule> surveyModules = ImmutableList.copyOf(surveyModuleDao.findByOrderByOrderNumberAsc());
        DomainInfosAndSurveyModulesResponse response = new DomainInfosAndSurveyModulesResponse();
        response.setDomainInfos(domainInfos.stream()
                .map(DomainInfo.TO_CLIENT_DOMAIN_INFO)
                .collect(Collectors.toList()));
        response.setSurveyModules(surveyModules.stream()
                .map(SurveyModule.TO_CLIENT_SURVEY_MODULE)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.CdrVersion> getCdrVersionUsed() {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        CdrVersion cdrVersion = cdrVersionDao.findByIsDefault(true);
        return ResponseEntity.ok(TO_CLIENT_CDR_VERSION.apply(cdrVersion));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getGenderAnalysis(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesAnalysis genderAnalysis = achillesAnalysisDao.findAnalysisById(GENDER_ANALYSIS);
        addGenderStratum(genderAnalysis,1, "0");
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(genderAnalysis));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getRaceAnalysis(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesAnalysis raceAnalysis = achillesAnalysisDao.findAnalysisById(RACE_ANALYSIS);
        addRaceStratum(raceAnalysis);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(raceAnalysis));
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.Analysis> getEthnicityAnalysis(){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesAnalysis ethnicityAnalysis = achillesAnalysisDao.findAnalysisById(ETHNICITY_ANALYSIS);
        addEthnicityStratum(ethnicityAnalysis);
        return ResponseEntity.ok(TO_CLIENT_ANALYSIS.apply(ethnicityAnalysis));
    }

    @Override
    public ResponseEntity<QuestionConceptListResponse> getSurveyResults(String surveyConceptId) {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        /* Set up the age and gender names */
        // Too slow and concept names wrong so we hardcode list
        // List<Concept> genders = conceptDao.findByConceptClassId("Gender");

        long longSurveyConceptId = Long.parseLong(surveyConceptId);

        // Gets all the questions of each survey
        List<QuestionConcept> questions = questionConceptDao.findSurveyQuestions(surveyConceptId);

        // Get survey definition
        QuestionConceptListResponse resp = new QuestionConceptListResponse();

        SurveyModule surveyModule = surveyModuleDao.findByConceptId(longSurveyConceptId);

        resp.setSurvey(SurveyModule.TO_CLIENT_SURVEY_MODULE.apply(surveyModule));
        // Get all analyses for question list and put the analyses on the question objects
        if (!questions.isEmpty()) {
            // Put ids in array for query to get all results at once
            List<String> qlist = new ArrayList();
            for (QuestionConcept q : questions) {
                qlist.add(String.valueOf(q.getConceptId()));
            }

            List<AchillesAnalysis> analyses = achillesAnalysisDao.findSurveyAnalysisResults(surveyConceptId, qlist);
            QuestionConcept.mapAnalysesToQuestions(questions, analyses);
        }

        List<QuestionConcept> subQuestions = questions.stream().
                filter(q -> q.getQuestions().get(0).getSub() == 1).collect(Collectors.toList());

        List<org.pmiops.workbench.model.QuestionConcept> convertedQuestions = questions.stream().map(TO_CLIENT_QUESTION_CONCEPT).collect(Collectors.toList());

        Collections.sort(subQuestions, (QuestionConcept q1, QuestionConcept q2) -> q1.getQuestions().get(0).getId() - q2.getQuestions().get(0).getId());

        for(QuestionConcept q: subQuestions) {
            List<SurveyQuestionMap> questionPaths = q.getQuestions();
            for(SurveyQuestionMap sqm: questionPaths) {
                List<Integer> conceptPath = Arrays.asList(sqm.getPath().split("\\.")).stream().map(Integer::valueOf).collect(Collectors.toList());
                if (conceptPath.size() == 3) {
                    int questionConceptId = conceptPath.get(0);
                    int resultConceptId = conceptPath.get(1);
                    org.pmiops.workbench.model.QuestionConcept mainQuestion = convertedQuestions.stream().filter(mq -> mq.getConceptId() == questionConceptId).collect(Collectors.toList()).get(0);
                    SurveyQuestionResult matchingSurveyResult = mainQuestion.getCountAnalysis().getSurveyQuestionResults().stream().filter(mr -> mr.getStratum3().equals(String.valueOf(resultConceptId))).collect(Collectors.toList()).get(0);
                    List<org.pmiops.workbench.model.QuestionConcept> mappedSubQuestions = matchingSurveyResult.getSubQuestions();
                    if (mappedSubQuestions == null) {
                        mappedSubQuestions = new ArrayList<>();
                    }
                    mappedSubQuestions.add(TO_CLIENT_QUESTION_CONCEPT.apply(q));
                    matchingSurveyResult.setSubQuestions(mappedSubQuestions);
                } else if (conceptPath.size() == 5) {
                    int questionConceptId1 = conceptPath.get(0);
                    int resultConceptId1 = conceptPath.get(1);
                    int resultConceptId2 = conceptPath.get(3);

                    org.pmiops.workbench.model.QuestionConcept mainQuestion1 = convertedQuestions.stream().filter(mq -> mq.getConceptId() == questionConceptId1).collect(Collectors.toList()).get(0);
                    SurveyQuestionResult matchingSurveyResult1 = mainQuestion1.getCountAnalysis().getSurveyQuestionResults().stream().filter(mr -> mr.getStratum3().equals(String.valueOf(resultConceptId1))).collect(Collectors.toList()).get(0);
                    List<org.pmiops.workbench.model.QuestionConcept> mainQuestion2List = matchingSurveyResult1.getSubQuestions();
                    if(mainQuestion2List != null) {
                        for(org.pmiops.workbench.model.QuestionConcept mainQuestion2: mainQuestion2List) {
                            List<SurveyQuestionResult> matchingSurveyResults2 = mainQuestion2.getCountAnalysis().getSurveyQuestionResults().stream().filter(mr -> mr.getStratum3().equals(String.valueOf(resultConceptId2))).collect(Collectors.toList());
                            if (matchingSurveyResults2.size() > 0 && mainQuestion2.getConceptId() == conceptPath.get(2).longValue()) {
                                List<org.pmiops.workbench.model.QuestionConcept> mappedSubQuestions = matchingSurveyResults2.get(0).getSubQuestions();
                                if (mappedSubQuestions == null) {
                                    mappedSubQuestions = new ArrayList<>();
                                }
                                mappedSubQuestions.add(TO_CLIENT_QUESTION_CONCEPT.apply(q));
                                matchingSurveyResults2.get(0).setSubQuestions(mappedSubQuestions);
                            }
                        }
                    }
                }
            }
        }

        resp.setItems(convertedQuestions.stream().filter(q -> q.getQuestions().get(0).getSub() == 0).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<ConceptAnalysisListResponse> getConceptAnalysisResults(List<String> conceptIds, String domainId){
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        ConceptAnalysisListResponse resp=new ConceptAnalysisListResponse();
        List<ConceptAnalysis> conceptAnalysisList=new ArrayList<>();
        List<Long> analysisIds  = new ArrayList<>();
        analysisIds.add(GENDER_ANALYSIS_ID);
        analysisIds.add(GENDER_IDENTITY_ANALYSIS_ID);
        analysisIds.add(RACE_ETHNICITY_ANALYSIS_ID);
        analysisIds.add(AGE_ANALYSIS_ID);
        analysisIds.add(RACE_ANALYSIS_ID);
        analysisIds.add(COUNT_ANALYSIS_ID);
        analysisIds.add(ETHNICITY_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_GENDER_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_DIST_ANALYSIS_ID);
        analysisIds.add(MEASUREMENT_GENDER_UNIT_ANALYSIS_ID);

        List<AchillesResultDist> overallDistResults = achillesResultDistDao.fetchByAnalysisIdsAndConceptIds(new ArrayList<Long>( Arrays.asList(MEASUREMENT_GENDER_DIST_ANALYSIS_ID) ),conceptIds);

        Multimap<Long, AchillesResultDist> distResultsByAnalysisId = null;
        if(overallDistResults != null){
            distResultsByAnalysisId = Multimaps
                    .index(overallDistResults, AchillesResultDist::getAnalysisId);
        }

        HashMap<Long,HashMap<String,List<AchillesResultDist>>> analysisDistResults = new HashMap<>();

        for(Long key:distResultsByAnalysisId.keySet()){
            Multimap<String,AchillesResultDist> conceptDistResults = Multimaps.index(distResultsByAnalysisId.get(key),AchillesResultDist::getStratum1);
            for(String concept:conceptDistResults.keySet()) {
                if(analysisDistResults.containsKey(key)){
                    HashMap<String,List<AchillesResultDist>> results = analysisDistResults.get(key);
                    results.put(concept,new ArrayList<>(conceptDistResults.get(concept)));
                }else{
                    HashMap<String,List<AchillesResultDist>> results = new HashMap<>();
                    results.put(concept,new ArrayList<>(conceptDistResults.get(concept)));
                    analysisDistResults.put(key,results);
                }
            }
        }
        for(String conceptId: conceptIds){
            ConceptAnalysis conceptAnalysis=new ConceptAnalysis();

            boolean isMeasurement = false;

            List<AchillesAnalysis> analysisList = achillesAnalysisDao.findConceptAnalysisResults(conceptId,analysisIds);

            HashMap<Long, AchillesAnalysis> analysisHashMap = new HashMap<>();
            for(AchillesAnalysis aa: analysisList){
                this.entityManager.detach(aa);
                analysisHashMap.put(aa.getAnalysisId(), aa);
            }

            conceptAnalysis.setConceptId(conceptId);
            Iterator it = analysisHashMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                Long analysisId = (Long)pair.getKey();
                AchillesAnalysis aa = (AchillesAnalysis)pair.getValue();

                //aa.setUnitName(unitName);
                if(analysisId != MEASUREMENT_GENDER_UNIT_ANALYSIS_ID && analysisId != MEASUREMENT_GENDER_ANALYSIS_ID && analysisId != MEASUREMENT_DIST_ANALYSIS_ID && !Strings.isNullOrEmpty(domainId)) {
                    aa.setResults(aa.getResults().stream().filter(ar -> ar.getStratum3().equalsIgnoreCase(domainId)).collect(Collectors.toList()));
                }
                if (analysisId == COUNT_ANALYSIS_ID) {
                    conceptAnalysis.setCountAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == GENDER_ANALYSIS_ID){
                    addGenderStratum(aa,2, conceptId);
                    conceptAnalysis.setGenderAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == GENDER_IDENTITY_ANALYSIS_ID){
                    addGenderIdentityStratum(aa);
                    conceptAnalysis.setGenderIdentityAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == RACE_ETHNICITY_ANALYSIS_ID){
                    addRaceEthnicityStratum(aa);
                    conceptAnalysis.setRaceEthnicityAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == AGE_ANALYSIS_ID){
                    addAgeStratum(aa, conceptId);
                    conceptAnalysis.setAgeAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == RACE_ANALYSIS_ID){
                    addRaceStratum(aa);
                    conceptAnalysis.setRaceAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == ETHNICITY_ANALYSIS_ID){
                    addEthnicityStratum(aa);
                    conceptAnalysis.setEthnicityAnalysis(TO_CLIENT_ANALYSIS.apply(aa));
                }else if(analysisId == MEASUREMENT_GENDER_ANALYSIS_ID){
                    Map<String,List<AchillesResult>> results = seperateUnitResults(aa);
                    List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                    HashMap<String,List<AchillesResultDist>> distResults = analysisDistResults.get(MEASUREMENT_GENDER_DIST_ANALYSIS_ID);
                    if (distResults != null) {
                        List<AchillesResultDist> conceptDistResults = distResults.get(conceptId);
                        if(conceptDistResults != null){
                            Multimap<String,AchillesResultDist> unitDistResults = Multimaps.index(conceptDistResults,AchillesResultDist::getStratum2);
                            for(String unit: unitDistResults.keySet()){
                                if (results.keySet().contains(unit)) {
                                    AchillesAnalysis unitGenderAnalysis = new AchillesAnalysis(aa);
                                    unitGenderAnalysis.setResults(results.get(unit));
                                    unitGenderAnalysis.setUnitName(unit);
                                    if(!unit.equalsIgnoreCase("no unit")) {
                                        processMeasurementGenderMissingBins(MEASUREMENT_GENDER_DIST_ANALYSIS_ID,unitGenderAnalysis, conceptId, unit, new ArrayList<>(unitDistResults.get(unit)), "numeric");
                                    } else {
                                        ArrayList<AchillesResult> textValues = new ArrayList<>();
                                        ArrayList<AchillesResult> numericValues = new ArrayList<>();
                                        // In case no unit has a mix of text and numeric values, only display text values as mix does not make sense to user.
                                        for (AchillesResult result: unitGenderAnalysis.getResults()) {
                                            if (NumberUtils.isNumber(result.getStratum4())) {
                                                numericValues.add(result);
                                            } else {
                                                textValues.add(result);
                                            }
                                        }
                                        if (textValues.size() > 0 && numericValues.size() > 0) {
                                            List<AchillesResult> filteredNumericResults = unitGenderAnalysis.getResults().stream().filter(ele -> textValues.stream()
                                                    .anyMatch(element -> element.getId()==ele.getId())).collect(Collectors.toList());
                                            unitGenderAnalysis.setResults(filteredNumericResults);
                                            processMeasurementGenderMissingBins(MEASUREMENT_GENDER_DIST_ANALYSIS_ID,unitGenderAnalysis, conceptId, null, null, "text");
                                        } else if (numericValues.size() > 0) {
                                            processMeasurementGenderMissingBins(MEASUREMENT_GENDER_DIST_ANALYSIS_ID,unitGenderAnalysis, conceptId, null, null, "numeric");
                                        }
                                    }
                                    unitSeperateAnalysis.add(unitGenderAnalysis);
                                }
                            }
                        }else {
                            unitSeperateAnalysis.add(aa);
                        }
                    }
                    addGenderStratum(aa,3, conceptId);
                    isMeasurement = true;
                    conceptAnalysis.setMeasurementValueGenderAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
                }else if(analysisId == MEASUREMENT_GENDER_UNIT_ANALYSIS_ID){
                    Map<String,List<AchillesResult>> results = seperateUnitResults(aa);
                    List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                    for(String unit: results.keySet()){
                        AchillesAnalysis unitGenderCountAnalysis = new AchillesAnalysis(aa);
                        unitGenderCountAnalysis.setResults(results.get(unit));
                        unitGenderCountAnalysis.setUnitName(unit);
                        unitSeperateAnalysis.add(unitGenderCountAnalysis);
                    }
                    isMeasurement = true;
                    conceptAnalysis.setMeasurementGenderCountAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
                }
            }

            if(isMeasurement){
                AchillesAnalysis measurementDistAnalysis = achillesAnalysisDao.findAnalysisById(MEASUREMENT_DIST_ANALYSIS_ID);
                List<AchillesResultDist> achillesResultDistList = achillesResultDistDao.fetchConceptDistResults(MEASUREMENT_DIST_ANALYSIS_ID,conceptId);
                HashMap<String,List<AchillesResultDist>> results = seperateDistResultsByUnit(achillesResultDistList);
                List<AchillesAnalysis> unitSeperateAnalysis = new ArrayList<>();
                for(String unit: results.keySet()){
                    AchillesAnalysis mDistAnalysis = new AchillesAnalysis(measurementDistAnalysis);
                    mDistAnalysis.setDistResults(results.get(unit));
                    mDistAnalysis.setUnitName(unit);
                    unitSeperateAnalysis.add(mDistAnalysis);
                }
                conceptAnalysis.setMeasurementDistributionAnalysis(unitSeperateAnalysis.stream().map(TO_CLIENT_ANALYSIS).collect(Collectors.toList()));
            }
            conceptAnalysisList.add(conceptAnalysis);
        }
        resp.setItems(conceptAnalysisList.stream().map(TO_CLIENT_CONCEPTANALYSIS).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    /**
     * This method gets concepts with maps to relationship in concept relationship table
     *
     * @param conceptId
     * @return
     */
    @Override
    public ResponseEntity<ConceptListResponse> getSourceConcepts(Long conceptId,Integer minCount) {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        Integer count=minCount;
        if(count == null){
            count = 0;
        }
        List<Concept> conceptList = conceptDao.findSourceConcepts(conceptId,count);
        ConceptListResponse resp = new ConceptListResponse();
        resp.setItems(conceptList.stream().map(TO_CLIENT_CONCEPT).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<org.pmiops.workbench.model.AchillesResult> getParticipantCount() {
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(defaultCdrVersionProvider.get());
        AchillesResult result = achillesResultDao.findAchillesResultByAnalysisId(PARTICIPANT_COUNT_ANALYSIS_ID);
        return ResponseEntity.ok(TO_CLIENT_ACHILLES_RESULT.apply(result));
    }

    public TreeSet<Float> makeBins(Float min,Float max) {
        TreeSet<Float> bins = new TreeSet<>();
        float binWidth = (max-min)/11;

        if (min >= 10 && max >= 10) {
            double tempBinWidth = Math.ceil(binWidth/10)*10;
            binWidth = (float)tempBinWidth;
        } else if ((max-min) <= 1 && (max-min) >= 0.1) {
            double tempBinWidth = Math.ceil(binWidth/0.1)*0.1;
            binWidth = (float)tempBinWidth;
        }

        bins.add(Float.valueOf(String.format("%.2f", min+binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+2*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+3*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+4*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+5*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+6*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+7*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+8*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+9*binWidth)));
        bins.add(Float.valueOf(String.format("%.2f", min+10*binWidth)));
        bins.add(max);
        return bins;
    }

    public void addGenderStratum(AchillesAnalysis aa, int stratum, String conceptId){
        Set<String> uniqueGenderStratums = new TreeSet<String>();
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName =ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                if (stratum == 1) {
                    uniqueGenderStratums.add(ar.getStratum1());
                    ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum1()));
                } else if (stratum == 2) {
                    uniqueGenderStratums.add(ar.getStratum2());
                    ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum2()));
                } else if (stratum == 3) {
                    uniqueGenderStratums.add(ar.getStratum3());
                    ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum3()));
                }
            }
        }
        if(uniqueGenderStratums.size() < 3) {
            Set<String> completeGenderStratumList = new TreeSet<String>(Arrays.asList(new String[] {"8507", "8532", "0"}));
            completeGenderStratumList.removeAll(uniqueGenderStratums);
            for(String missingGender: completeGenderStratumList){
                AchillesResult missingResult = null;
                if (stratum == 1) {
                    missingResult = new AchillesResult(aa.getAnalysisId(), missingGender, null, null, null, null, 20L, 20L);
                } else if (stratum == 2) {
                    missingResult = new AchillesResult(aa.getAnalysisId(), conceptId, missingGender, null, null, null, 20L, 20L);
                } else if (stratum == 3) {
                    missingResult = new AchillesResult(aa.getAnalysisId(), conceptId, null, missingGender, null, null, 20L, 20L);
                }
                missingResult.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(missingGender));
                aa.getResults().add(missingResult);
            }
        }
        aa.setResults(aa.getResults().stream().filter(ar -> ar.getAnalysisStratumName() != null).collect(Collectors.toList()));
    }

    public void addGenderIdentityStratum(AchillesAnalysis aa){
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName =ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.genderIdentityStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void addAgeStratum(AchillesAnalysis aa, String conceptId){
        Set<String> uniqueAgeDeciles = new TreeSet<String>();
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if (ar.getStratum2() != null && !ar.getStratum2().equals("0")) {
                uniqueAgeDeciles.add(ar.getStratum2());
            }
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(ar.getStratum2()));
            }
        }
        aa.setResults(aa.getResults().stream().filter(ar -> ar.getAnalysisStratumName() != null).collect(Collectors.toList()));
        if(uniqueAgeDeciles.size() < 8){
            Set<String> completeAgeDeciles = new TreeSet<String>(Arrays.asList(new String[] {"2", "3", "4", "5", "6", "7", "8", "9"}));
            completeAgeDeciles.removeAll(uniqueAgeDeciles);
            for(String missingAgeDecile: completeAgeDeciles){
                AchillesResult missingResult = new AchillesResult(AGE_ANALYSIS_ID, conceptId, missingAgeDecile, null, null, null, 20L, 20L);
                missingResult.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(missingAgeDecile));
                aa.getResults().add(missingResult);
            }
        }
    }

    public void addRaceStratum(AchillesAnalysis aa) {
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.raceStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void addRaceEthnicityStratum(AchillesAnalysis aa){
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.raceEthnicityStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void addEthnicityStratum(AchillesAnalysis aa) {
        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.raceStratumNameMap.get(ar.getStratum2()));
            }
        }
    }

    public void processMeasurementGenderMissingBins(Long analysisId, AchillesAnalysis aa, String conceptId, String unitName, List<AchillesResultDist> resultDists, String type) {

        if (resultDists != null) {
            Float maleBinMin = null;
            Float maleBinMax = null;

            Float femaleBinMin = null;
            Float femaleBinMax = null;

            Float intersexBinMin = null;
            Float intersexBinMax = null;

            Float noneBinMin = null;
            Float noneBinMax = null;

            Float otherBinMin = null;
            Float otherBinMax = null;

            for(AchillesResultDist ard:resultDists){
                if(Integer.parseInt(ard.getStratum3())== MALE) {
                    maleBinMin = Float.valueOf(ard.getStratum4());
                    maleBinMax = Float.valueOf(ard.getStratum5());
                }
                else if(Integer.parseInt(ard.getStratum3()) == FEMALE) {
                    femaleBinMin = Float.valueOf(ard.getStratum4());
                    femaleBinMax = Float.valueOf(ard.getStratum5());
                }
                else if(Integer.parseInt(ard.getStratum3()) == INTERSEX) {
                    intersexBinMin = Float.valueOf(ard.getStratum4());
                    intersexBinMax = Float.valueOf(ard.getStratum5());
                }
                else if(Integer.parseInt(ard.getStratum3()) == NONE) {
                    noneBinMin = Float.valueOf(ard.getStratum4());
                    noneBinMax = Float.valueOf(ard.getStratum5());
                }
                else if(Integer.parseInt(ard.getStratum3()) == OTHER) {
                    otherBinMin = Float.valueOf(ard.getStratum4());
                    otherBinMax = Float.valueOf(ard.getStratum5());
                }
            }

            if (femaleBinMax == null && femaleBinMin == null) {
                if (maleBinMin != null && maleBinMax != null && otherBinMax != null && otherBinMin != null) {
                    femaleBinMin = Math.min(maleBinMin, otherBinMin);
                    femaleBinMax = Math.max(maleBinMax, otherBinMax);
                } else if (maleBinMin != null && maleBinMax != null) {
                    femaleBinMin = maleBinMin;
                    femaleBinMax = maleBinMax;
                } else if (otherBinMax != null && otherBinMin != null) {
                    femaleBinMin = otherBinMin;
                    femaleBinMax = otherBinMax;
                }
            }

            if (maleBinMax == null && maleBinMin == null) {
                if (femaleBinMin != null && femaleBinMax != null && otherBinMax != null && otherBinMin != null) {
                    maleBinMin = Math.min(femaleBinMin, otherBinMin);
                    maleBinMax = Math.max(femaleBinMax, otherBinMax);
                } else if (femaleBinMin != null && femaleBinMax != null) {
                    maleBinMin = femaleBinMin;
                    maleBinMax = femaleBinMax;
                } else if (otherBinMax != null && otherBinMin != null) {
                    maleBinMin = otherBinMin;
                    maleBinMax = otherBinMax;
                }
            }

            if (otherBinMax == null && otherBinMin == null) {
                if (femaleBinMin != null && femaleBinMax != null && maleBinMax != null && maleBinMin != null) {
                    otherBinMin = Math.min(femaleBinMin, maleBinMin);
                    otherBinMax = Math.max(femaleBinMax, maleBinMax);
                } else if (femaleBinMin != null && femaleBinMax != null) {
                    otherBinMin = femaleBinMin;
                    otherBinMax = femaleBinMax;
                } else if (maleBinMin != null && maleBinMax != null) {
                    otherBinMax = maleBinMax;
                    otherBinMin = maleBinMin;
                }
            }

            TreeSet<Float> maleBinRanges = new TreeSet<Float>();
            TreeSet<Float> femaleBinRanges = new TreeSet<Float>();
            TreeSet<Float> intersexBinRanges = new TreeSet<Float>();
            TreeSet<Float> noneBinRanges = new TreeSet<Float>();
            TreeSet<Float> otherBinRanges = new TreeSet<Float>();

            if(maleBinMax != null && maleBinMin != null){
                maleBinRanges = makeBins(maleBinMin, maleBinMax);
            }

            if(femaleBinMax != null && femaleBinMin != null){
                femaleBinRanges = makeBins(femaleBinMin, femaleBinMax);
            }

            if(intersexBinMax != null && intersexBinMin != null){
                intersexBinRanges = makeBins(intersexBinMin, intersexBinMax);
            }

            if(noneBinMax != null && noneBinMin != null){
                noneBinRanges = makeBins(noneBinMin, noneBinMax);
            }

            if(otherBinMax != null && otherBinMin != null){
                otherBinRanges = makeBins(otherBinMin, otherBinMax);
            }

            for(AchillesResult ar: aa.getResults()){
                String analysisStratumName=ar.getAnalysisStratumName();
                if(Long.valueOf(ar.getStratum3()) == MALE && maleBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                    maleBinRanges.remove(Float.parseFloat(ar.getStratum4()));
                }else if(Long.valueOf(ar.getStratum3()) == FEMALE && femaleBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                    femaleBinRanges.remove(Float.parseFloat(ar.getStratum4()));
                }else if(Long.valueOf(ar.getStratum3()) == INTERSEX && intersexBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                    intersexBinRanges.remove(Float.parseFloat(ar.getStratum4()));
                }else if(Long.valueOf(ar.getStratum3()) == NONE && noneBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                    noneBinRanges.remove(Float.parseFloat(ar.getStratum4()));
                }else if(Long.valueOf(ar.getStratum3()) == OTHER && otherBinRanges.contains(Float.parseFloat(ar.getStratum4()))){
                    otherBinRanges.remove(Float.parseFloat(ar.getStratum4()));
                }
                if (analysisStratumName == null || analysisStratumName.equals("")) {
                    ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum3()));
                }
            }

            for(float maleRemaining: maleBinRanges){
                String missingValue = String.format("%.2f", maleRemaining);
                AchillesResult achillesResult = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(MALE), missingValue, null, 20L, 20L);
                aa.addResult(achillesResult);
            }

            for(float femaleRemaining: femaleBinRanges){
                String missingValue = String.format("%.2f", femaleRemaining);
                AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(FEMALE), missingValue, null, 20L, 20L);
                aa.addResult(ar);
            }

            for(float intersexRemaining: intersexBinRanges){
                String missingValue = String.format("%.2f", intersexRemaining);
                AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(INTERSEX), missingValue, null, 20L, 20L);
                aa.addResult(ar);
            }

            for(float noneRemaining: noneBinRanges){
                String missingValue = String.format("%.2f", noneRemaining);
                AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(NONE), missingValue, null, 20L, 20L);
                aa.addResult(ar);
            }

            for(float otherRemaining: otherBinRanges){
                String missingValue = String.format("%.2f", otherRemaining);
                AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(OTHER), missingValue, null, 20L, 20L);
                aa.addResult(ar);
            }
        } else {

            List<AchillesResult> maleResults = new ArrayList<>();
            List<AchillesResult> femaleResults = new ArrayList<>();
            List<AchillesResult> otherResults = new ArrayList<>();

            for(AchillesResult ar: aa.getResults()){
                String analysisStratumName=ar.getAnalysisStratumName();
                if(Long.valueOf(ar.getStratum3()) == MALE ){
                    maleResults.add(ar);
                }else if(Long.valueOf(ar.getStratum3()) == FEMALE ){
                    femaleResults.add(ar);
                }else if(Long.valueOf(ar.getStratum3()) == OTHER ){
                    otherResults.add(ar);
                }
                if (analysisStratumName == null || analysisStratumName.equals("")) {
                    ar.setAnalysisStratumName(QuestionConcept.genderStratumNameMap.get(ar.getStratum2()));
                }
            }

            if (type.equals("numeric")) {
                if (maleResults.size() == 0) {
                    AchillesResult achillesResult = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, "No Unit", String.valueOf(MALE), "0", null, 20L, 20L);
                    aa.addResult(achillesResult);
                }
                if (femaleResults.size() == 0) {
                    AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(FEMALE), "0", null, 20L, 20L);
                    aa.addResult(ar);
                }
                if (otherResults.size() == 0) {
                    AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(OTHER), "0", null, 20L, 20L);
                    aa.addResult(ar);
                }
            } else if(type.equals("text")) {
                if (maleResults.size() == 0) {
                    AchillesResult achillesResult = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, "No Unit", String.valueOf(MALE), "Null", null, 20L, 20L);
                    aa.addResult(achillesResult);
                }
                if (femaleResults.size() == 0) {
                    AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(FEMALE), "Null", null, 20L, 20L);
                    aa.addResult(ar);
                }
                if (otherResults.size() == 0) {
                    AchillesResult ar = new AchillesResult(MEASUREMENT_GENDER_ANALYSIS_ID, conceptId, unitName, String.valueOf(OTHER), "Null", null, 20L, 20L);
                    aa.addResult(ar);
                }
            }

        }

    }

    public static HashMap<String,List<AchillesResult>> seperateUnitResults(AchillesAnalysis aa){
        List<String> distinctUnits = new ArrayList<>();

        for(AchillesResult ar:aa.getResults()){
            if(!distinctUnits.contains(ar.getStratum2()) && !Strings.isNullOrEmpty(ar.getStratum2())){
                distinctUnits.add(ar.getStratum2());
            }
        }

        Multimap<String, AchillesResult> resultsWithUnits = Multimaps
                .index(aa.getResults(), AchillesResult::getStratum2);

        HashMap<String,List<AchillesResult>> seperatedResults = new HashMap<>();

        for(String key:resultsWithUnits.keySet()){
            seperatedResults.put(key,new ArrayList<>(resultsWithUnits.get(key)));
        }
        return seperatedResults;
    }

    public static HashMap<String,List<AchillesResultDist>> seperateDistResultsByUnit(List<AchillesResultDist> results) {
        Multimap<String, AchillesResultDist> distResultsWithUnits = Multimaps
                .index(results, AchillesResultDist::getStratum2);
        HashMap<String,List<AchillesResultDist>> seperatedResults = new HashMap<>();

        for(String key:distResultsWithUnits.keySet()){
            seperatedResults.put(key,new ArrayList<>(distResultsWithUnits.get(key)));
        }

        return seperatedResults;
    }

    public void processMeasurementAgeDecileMissingBins(Long analysisId, AchillesAnalysis aa, String conceptId, String unitNam, List<AchillesResultDist> distRows) {

        HashMap<String,ArrayList<Float>>  decileRanges = new HashMap<>();

        for(AchillesResultDist ard:distRows){
            if(Integer.parseInt(ard.getStratum3())== '2') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("2",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '3') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("3",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '4') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("4",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '5') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("5",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '6') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("6",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '7') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("7",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
            else if(Integer.parseInt(ard.getStratum3()) == '8') {
                Float binMin = Float.valueOf(ard.getStratum4());
                Float binMax = Float.valueOf(ard.getStratum5());
                decileRanges.put("8",new ArrayList<Float>( Arrays.asList(binMin,binMax) ));
            }
        }

        TreeSet<Float> binRanges2 = new TreeSet<Float>();
        TreeSet<Float> binRanges3 = new TreeSet<Float>();
        TreeSet<Float> binRanges4 = new TreeSet<Float>();
        TreeSet<Float> binRanges5 = new TreeSet<Float>();
        TreeSet<Float> binRanges6 = new TreeSet<Float>();
        TreeSet<Float> binRanges7 = new TreeSet<Float>();
        TreeSet<Float> binRanges8 = new TreeSet<Float>();


        if(decileRanges.get("2") != null){
            binRanges2 = makeBins(decileRanges.get("2").get(0), decileRanges.get("2").get(1));
        }
        if(decileRanges.get("3") != null){
            binRanges3 = makeBins(decileRanges.get("3").get(0), decileRanges.get("3").get(1));
        }
        if(decileRanges.get("4") != null){
            binRanges4 = makeBins(decileRanges.get("4").get(0), decileRanges.get("4").get(1));
        }
        if(decileRanges.get("5") != null){
            binRanges5 = makeBins(decileRanges.get("5").get(0), decileRanges.get("5").get(1));
        }
        if(decileRanges.get("6") != null){
            binRanges6 = makeBins(decileRanges.get("6").get(0), decileRanges.get("6").get(1));
        }
        if(decileRanges.get("7") != null){
            binRanges7 = makeBins(decileRanges.get("7").get(0), decileRanges.get("7").get(1));
        }
        if(decileRanges.get("8") != null){
            binRanges8 = makeBins(decileRanges.get("8").get(0), decileRanges.get("8").get(1));
        }

        for(AchillesResult ar: aa.getResults()){
            String analysisStratumName=ar.getAnalysisStratumName();
            if(ar.getStratum2().equals("2") && binRanges2.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges2.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("3") && binRanges3.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges3.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("4") && binRanges4.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges4.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("5") && binRanges5.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges5.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("6") && binRanges6.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges6.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("7") && binRanges7.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges7.remove(Float.parseFloat(ar.getStratum4()));
            }else if(ar.getStratum2().equals("8") && binRanges8.contains(Float.parseFloat(ar.getStratum4()))){
                binRanges8.remove(Float.parseFloat(ar.getStratum4()));
            }

            if (analysisStratumName == null || analysisStratumName.equals("")) {
                ar.setAnalysisStratumName(QuestionConcept.ageStratumNameMap.get(ar.getStratum2()));
            }
        }

    }
}