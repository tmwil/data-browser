package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.VocabularyCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConceptDao extends CrudRepository<Concept, Long> {

    @Query(nativeQuery=true, value="select c.* from concept c join concept_relationship cr on c.concept_id=cr.concept_id_2 " +
            "where cr.concept_id_1=?1 and cr.relationship_id='Maps to' and c.can_select=1")
    List<Concept> findStandardConcepts(long concept_id);

    @Query(value="select c.* from concept c "+
            "join concept_relationship rel on " +
            "rel.concept_id_1 = c.concept_id and rel.concept_id_2 = :conceptId and " +
            "rel.relationship_id = 'maps to' where c.source_count_value > :minCount order " +
            "by c.count_value desc",nativeQuery=true)
    List<Concept> findSourceConcepts(@Param("conceptId") long conceptId,@Param("minCount") Integer minCount);

    @Query(value = "select c.* from concept c " +
            "join concept_relationship rel on rel.concept_id_2 = c.concept_id " +
            "and rel.concept_id_1 = :conceptId and rel.relationship_id = 'maps to' " +
            "where c.concept_id != :conceptId order by c.count_value desc",
            nativeQuery = true)
    List<Concept> findConceptsMapsToParents(@Param("conceptId") long conceptId);

    List<Concept> findByConceptName(String conceptName);

    @Query(value = "select c.* from concept c " +
            "where c.vocabulary_id in ('Gender', 'Race', 'Ethnicity')",
            nativeQuery = true)
    List<Concept> findGenderRaceEthnicityFromConcept();

    /**
     * Return the number of standard concepts in each vocabulary for the specified domain matching the
     * specified expression, matching concept name, synonym, ID, or code.
     * @param matchExp SQL MATCH expression to match concept name or synonym
     * @param domainId domain ID to use when filtering concepts
     * @return per-vocabulary concept counts
     */
    @Query(value = "select c.vocabularyId as vocabularyId, count(distinct c.conceptId) as conceptCount from Concept c\n" +
            "where (c.countValue > 0 or c.sourceCountValue > 0) and\n" +
            "matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 and\n" +
            "c.standardConcept IN ('S', 'C') and\n" +
            "c.domainId = ?2\n" +
            "group by c.vocabularyId\n" +
            "order by c.vocabularyId\n")
    List<VocabularyCount> findVocabularyStandardConceptCounts(String matchExp, String domainId);

    /**
     * Return the number of concepts (standard or non-standard) in each vocabulary
     * for the specified domain matching the specified expression, matching concept name, synonym,
     * ID, or code.
     * @param matchExp SQL MATCH expression to match concept name or synonym
     * @param domainId domain ID to use when filtering concepts
     * @return per-vocabulary concept counts
     */
    @Query(value = "select c.vocabularyId as vocabularyId, count(*) as conceptCount from Concept c\n" +
            "where (c.countValue > 0 or c.sourceCountValue > 0) and\n" +
            "matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 and\n" +
            "c.domainId = ?2\n" +
            "group by c.vocabularyId\n" +
            "order by c.vocabularyId\n")
    List<VocabularyCount> findVocabularyAllConceptCounts(String matchExp, String domainId);

    /*
    @Query(value = "select distinct c1.* from concept_relationship cr " +
            "join concept c1 on (cr.concept_id_2 = c1.concept_id " +
            "and cr.concept_id_1 in (" +
            "select distinct c.concept_id from criteria c\n" +
            "where c.type = 'DRUG' " +
            "and c.subtype in ('BRAND') " +
            "and c.is_selectable = 1 " +
            "and match(c.name, c.code) against(?1 in boolean mode) > 0 " +
            "order by c.name asc) " +
            "and c1.concept_class_id = 'Ingredient' and (c1.has_counts > 0)) ", nativeQuery = true)
    List<Concept> findDrugIngredientsByBrand(String query);

    @Query(value = "select distinct c.concept_id from cb_criteria c\n" +
            "inner join ( \n" +
            "select cr.concept_id_2 from cb_criteria_relationship cr \n" +
            "join concept c1 on (cr.concept_id_2 = c1.concept_id\n" +
            "and cr.concept_id_1 in (select distinct concept_id from cb_criteria c where c.domain_id='DRUG' and c.type='BRAND' and match(c.name, c.code) against(?1 in boolean mode) > 0)\n" +
            "and c1.concept_class_id = 'Ingredient')) cr1 on c.concept_id = cr1.concept_id_2\n" +
            "and c.domain_id = 'DRUG' and c.type = 'RXNORM' and match(c.synonyms) against('+[drug_rank1]' in boolean mode)", nativeQuery= true)
    List<Long> findDrugIngredientsByBrand(String query);

    @Query(value = "select distinct c1.* from concept_relationship cr " +
            "join concept c1 on (cr.concept_id_2 = c1.concept_id " +
            "and cr.concept_id_1 in (" +
            "select distinct c.concept_id from criteria c\n" +
            "where c.type = 'DRUG' " +
            "and c.subtype in ('BRAND') " +
            "and c.is_selectable = 1 " +
            "and match(c.name, c.code) against(?1 in boolean mode) > 0 " +
            "order by c.name asc) " +
            "and c1.concept_class_id = 'Ingredient') and cr.concept_id_2 not in (?2) and (c1.has_counts > 0)", nativeQuery = true)
    List<Concept> findDrugIngredientsByBrandNotInConceptIds(String query, List<Long> conceptIds);
    */

    @Query(value = "select distinct cr.concept_id_2 from cb_criteria_relationship cr \n" +
            "join concept c1 on (cr.concept_id_2 = c1.concept_id\n" +
            "and cr.concept_id_1 in (select distinct concept_id from cb_criteria c where c.domain_id='DRUG' and c.type='BRAND' and match(c.name, c.code) against(?1 in boolean mode) > 0)\n" +
            "and c1.concept_class_id = 'Ingredient') join cb_criteria c on c.concept_id = cr.concept_id_2\n" +
            "and c.domain_id = 'DRUG' and c.type = 'RXNORM' and c.synonyms like '%drug_rank1%' ", nativeQuery= true)
    List<Long> findDrugIngredientsByBrand(String query);

    @Query(value = "select distinct c.concept_id from cb_criteria c\n" +
            "inner join ( \n" +
            "select cr.concept_id_2 from cb_criteria_relationship cr \n" +
            "join concept c1 on (cr.concept_id_2 = c1.concept_id\n" +
            "and cr.concept_id_1 in (select distinct concept_id from cb_criteria c where c.domain_id='DRUG' and c.type='BRAND' and match(c.name, c.code) against(?1 in boolean mode) > 0)\n" +
            "and c1.concept_class_id = 'Ingredient' and cr.concept_id_2 not in (?2)) ) cr1 on c.concept_id = cr1.concept_id_2\n" +
            "and c.domain_id = 'DRUG' and c.type = 'RXNORM' and c.synonyms like '%drug_rank1%' in boolean mode) ", nativeQuery= true)
    List<Long> findDrugIngredientsByBrandNotInConceptIds(String query, List<Long> conceptIds);

}
