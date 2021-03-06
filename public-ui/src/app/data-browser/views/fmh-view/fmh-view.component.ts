import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ISubscription} from 'rxjs/Subscription';
import {DataBrowserService} from '../../../../publicGenerated';
import {DbConfigService} from '../../../utils/db-config.service';
import {GraphType} from '../../../utils/enum-defs';
import {TooltipService} from '../../../utils/tooltip.service';

@Component({
  selector: 'app-fmh-view',
  templateUrl: './fmh-view.component.html',
  styleUrls: ['./fmh-view.component.css',
    '../../../styles/cards.css',
    '../../../styles/template.css',
    '../survey-view/survey-view.component.css']
})
export class FmhViewComponent implements OnInit {
  @Input() searchText: any;
  @Input() surveyCountAnalysis: any;
  conditionText: string;
  fmText: string;
  conditionQuestionFetchComplete = false;
  fmQuestionFetchComplete = false;
  graphButtons = ['Sex Assigned at Birth', 'Age When Survey Was Taken'];
  conditionQuestions = [];
  fmQuestions = [];
  @Input() participantCount: number;
  @Input() surveyName: number;
  questionOrder = {43528515: 1, 1384639: 2, 43528634: 3, 43528761: 4,
                   43529158: 5, 43529767: 6, 43529272: 7, 43529217: 8,
                   702786: 9, 43529966: 10, 43529638: 11};
  showAnswer = {};
  loading = false;
  private subscriptions: ISubscription[] = [];

  constructor(private api: DataBrowserService, public dbc: DbConfigService,
              private tooltipText: TooltipService) {
  }

  ngOnInit() {
    this.conditionText = 'For participants who selected "A lot" or "Some" in the first question, ' +
      'view family history by medical condition and/or event';
    this.fmText = 'For participants who selected "A lot" or "Some" in the first question, ' +
      'view family history by family member below.';
    this.getSurveyResults();
  }

  private getSurveyResults() {
    const conditionQuestionConceptIds = ['43528515', '1384639', '43528634', '43528761', '43529158', '43529767', '43529272', '43529217', '702786', '43529966', '43529638'];
    const fmQuestionConceptIds = ['43528764', '43528763', '43528649', '43528651', '43528650', '43528765'];
    this.subscriptions.push(this.api.getFMHGroupedQuestions('43528698',
      this.searchText.value, conditionQuestionConceptIds).subscribe({
      next: x => {
        this.processQuestions(x, 'condition');
      },
      error: err => {
        console.error('Observer got an error: ' + err);
        this.loading = false;
      },
      complete: () => { this.conditionQuestionFetchComplete = true; }
    }));
    this.subscriptions.push(this.api.getFMHGroupedQuestions('43528698',
      this.searchText.value, fmQuestionConceptIds).subscribe({
      next: x => {
        this.processQuestions(x, 'family_member');
      },
      error: err => {
        console.error('Observer got an error: ' + err);
        this.loading = false;
      },
      complete: () => { this.fmQuestionFetchComplete = true; }
    }));
    this.subscriptions.push(this.searchText.valueChanges
      .debounceTime(1000)
      .distinctUntilChanged()
      .switchMap((query) => this.api.getFMHGroupedQuestions('43528698',
        query, conditionQuestionConceptIds))
      .subscribe({
        next: results => {
          this.processQuestions(results, 'condition');
        },
        error: err => {
          console.log('Error searching: ', err);
          this.loading = false;
        }
      }));
    this.subscriptions.push(this.searchText.valueChanges
      .debounceTime(1000)
      .distinctUntilChanged()
      .switchMap((query) => this.api.getFMHGroupedQuestions('43528698',
        this.searchText.value, fmQuestionConceptIds))
      .subscribe({
        next: results => {
          this.processQuestions(results, 'family_member');
        },
        error: err => {
          console.log('Error searching: ', err);
          this.loading = false;
        }
      }));
  }

  public processQuestions(results: any, branching: string) {
    // Add Did not answer to each question
    for (const q of results.items) {
      this.showAnswer[q.conceptId] = false;
      q.actualQuestionNumber = 0;
      q.graphToShow = GraphType.BiologicalSex;
      q.selectedAnalysis = q.genderAnalysis;
      q.graphDataToShow = 'Count';
      q.questionResultFetchComplete = false;
      q.actualQuestionNumber = this.questionOrder[q.conceptId];
    }
    if (branching === 'condition') {
      this.conditionQuestions = results.items;
      this.conditionQuestions.sort((a1, a2) => {
        if (a1.actualQuestionNumber < a2.actualQuestionNumber) {
          return -1;
        }
        if (a1.actualQuestionNumber > a2.actualQuestionNumber) {
          return 1;
        }
        return 0;
      });
    } else {
      this.fmQuestions = results.items;
      this.fmQuestions.sort((a1, a2) => {
        if (a1.actualQuestionNumber < a2.actualQuestionNumber) {
          return -1;
        }
        if (a1.actualQuestionNumber > a2.actualQuestionNumber) {
          return 1;
        }
        return 0;
      });
    }
  }

  public toggleAnswer(q: any) {
    this.api.getFMHConditionMainResults(q.conceptId, q)
      .subscribe({
        next: results => {
          q.countAnalysis = results.countAnalysis;
          q.genderAnalysis = results.genderAnalysis;
          q.ageAnalysis = results.ageAnalysis;
          q.genderCountAnalysis = results.genderCountAnalysis;
          q.ageCountAnalysis = results.ageCountAnalysis;
          q.questionResultFetchComplete = true;
          this.processSurveyQuestionResults(q);
        },
        error: err => {
          console.log('Error searching: ', err);
          this.loading = false;
        }
      });
    if (!this.showAnswer[q.conceptId]) {
      this.showAnswer[q.conceptId] = true;
      q.expanded = true;
    } else {
      this.showAnswer[q.conceptId] = false;
      q.expanded = false;
    }
    if (this.showAnswer[q.conceptId]) {
      this.dbc.triggerEvent('conceptClick', 'Survey Question',
        'Expand to see answers',
        this.surveyName + ' - Q' + q.actualQuestionNumber + ' - '
        + q.conceptName, this.searchText.value, null);
    }
  }

  public processSurveyQuestionResults(q) {
    q.graphToShow = GraphType.BiologicalSex;
    q.selectedAnalysis = q.genderAnalysis;
    q.graphDataToShow = 'Count';
    for (const a of q.countAnalysis.surveyQuestionResults) {
      a.countPercent = this.countPercentage(a.countValue, this.participantCount);
      this.addMissingBiologicalSexResults(q.genderAnalysis,
        q.genderAnalysis.surveyQuestionResults.
        filter(r => r.stratum3 !== null && r.stratum3 === a.stratum3),
        this.participantCount);
      this.addMissingAgeResults(q.ageAnalysis,
        q.ageAnalysis.surveyQuestionResults.
        filter(r => r.stratum3 !== null && r.stratum3 === a.stratum3),
        this.participantCount);
      a.subQuestionFetchComplete = false;
    }
    q.countAnalysis.surveyQuestionResults.push(
      this.addDidNotAnswerResult(
        q.countAnalysis.surveyQuestionResults, this.participantCount));
    q.countAnalysis.surveyQuestionResults.sort((a1, a2) => {
      if (a1.countValue > a2.countValue) {
        return -1;
      }
      if (a1.countValue < a2.countValue) {
        return 1;
      }
      return 0;
    });
  }

  public countPercentage(countValue: number, totalCount: number) {
    if (!countValue || countValue <= 0) { return 0; }
    let percent: number = countValue / totalCount;
    percent = parseFloat(percent.toFixed(4));
    return percent * 100;
  }

  public addMissingBiologicalSexResults(genderAnalysis: any, results: any, totalCount: number) {
    const uniqueGenderStratums: string[] = [];
    const fullGenderStratums = ['8507', '8532', '0'];
    for (const result of results) {
      if (uniqueGenderStratums.indexOf(result.stratum5) <= -1) {
        uniqueGenderStratums.push(result.stratum5);
      }
    }
    const missingGenderStratums = fullGenderStratums.
    filter(item => uniqueGenderStratums.indexOf(item) < 0);
    for (const missingStratum of missingGenderStratums) {
      if (results.length > 0) {
        const missingResult = {
          analysisId: genderAnalysis.analysisId,
          countValue: 20,
          countPercent: this.countPercentage(20, totalCount),
          stratum1: results[0].stratum1,
          stratum2: results[0].stratum2,
          stratum3: results[0].stratum3,
          stratum4: results[0].stratum4,
          stratum5: missingStratum,
          stratum6: results[0].stratum6,
          analysisStratumName: this.dbc.GENDER_STRATUM_MAP[missingStratum]
        };
        genderAnalysis.surveyQuestionResults.push(missingResult);
      }
    }
  }

  public addMissingAgeResults(ageAnalysis: any, results: any, totalCount: number) {
    const uniqueAgeStratums: string[] = [];
    const fullAgeStratums = ['2', '3', '4', '5', '6', '7', '8', '9'];
    for (const result of results) {
      if (uniqueAgeStratums.indexOf(result.stratum5) <= -1) {
        uniqueAgeStratums.push(result.stratum5);
      }
    }
    const missingAgeStratums = fullAgeStratums.filter(item => uniqueAgeStratums.indexOf(item) < 0);
    for (const missingStratum of missingAgeStratums) {
      if (results.length > 0) {
        const missingResult = {
          analysisId: ageAnalysis.analysisId,
          countValue: 20,
          countPercent: this.countPercentage(20, totalCount),
          stratum1: results[0].stratum1,
          stratum2: results[0].stratum2,
          stratum3: results[0].stratum3,
          stratum4: results[0].stratum4,
          stratum5: missingStratum,
          stratum6: results[0].stratum6,
          analysisStratumName: this.dbc.AGE_STRATUM_MAP[missingStratum]
        };
        ageAnalysis.surveyQuestionResults.push(missingResult);
      }
    }
  }

  public addDidNotAnswerResult(results: any[], participantCount: number) {
    let didNotAnswerCount = participantCount;
    for (const r of results) {
      didNotAnswerCount = didNotAnswerCount - r.countValue;
    }
    const result = results[0];
    if (didNotAnswerCount <= 0) {
      didNotAnswerCount = 20;
    }
    const notAnswerPercent = this.countPercentage(didNotAnswerCount, participantCount);
    const didNotAnswerResult = {
      analysisId: result.analysisId,
      countValue: didNotAnswerCount,
      countPercent: notAnswerPercent,
      stratum1: result.stratum1,
      stratum2: result.stratum2,
      stratum3: '0',
      stratum4: 'Did not answer',
      stratum5: result.stratum5,
      stratum6: result.stratum6,
    };
    return didNotAnswerResult;
  }

  public showAnswerGraphs(a: any, q: any) {
    if (a.hasSubQuestions === 0) {
      this.showGraph(a);
    } else if (a.hasSubQuestions === 1) {
      this.getSubQuestions(a, 1);
    }
    a.expanded = !a.expanded;
    if (a.expanded) {
      if (a.stratum4.toLowerCase().indexOf('more than one race') > -1) {
        this.dbc.triggerEvent('conceptClick', 'More than one race/ethnicity view graphs',
          'Expand to see graphs', this.surveyName + ' - Q'
          + q.actualQuestionNumber + ' - ' + q.conceptName + ' - ' + a.stratum4
          , this.searchText.value, null);
      }
      this.dbc.triggerEvent('conceptClick', 'View Graphs',
        'Expand to see graphs', this.surveyName + ' - Q'
        + q.actualQuestionNumber + ' - ' + q.conceptName + ' - ' + a.stratum4 +
        ' - ' + ' Icon', this.searchText.value, null);
      this.dbc.triggerEvent('conceptClick', 'View Graphs',
        'Expand to see graphs', this.surveyName + ' - Q'
        + q.actualQuestionNumber + ' - ' + q.conceptName + ' - ' + a.stratum4 +
        ' - ' + 'Sex Assigned at Birth', this.searchText.value, null);
    }
  }

  public showGraph(a) {
    a.subQuestionFetchComplete = true;
  }
  public getSubQuestions(a: any, level: number) {
    this.api.getFMHSurveyQuestionResults(a.stratum3)
      .subscribe({
        next: results => {
          a.subQuestions = results.items;
          for (const subQuestion of a.subQuestions) {
            subQuestion.selectedAnalysis = subQuestion.genderAnalysis;
            subQuestion.actualQuestionNumber = 0;
            if (subQuestion.questions && subQuestion.questions.length > 0) {
              subQuestion.actualQuestionNumber =
                subQuestion.questions[0]['questionOrderNumber'];
            }
            subQuestion.graphToShow = GraphType.BiologicalSex;
            subQuestion.graphDataToShow = 'Count';
            subQuestion.countAnalysis.surveyQuestionResults =
              subQuestion.countAnalysis.surveyQuestionResults.
              filter(r => r.stratum6.indexOf(a.stratum3) > -1);
            subQuestion.genderAnalysis.surveyQuestionResults =
              subQuestion.genderAnalysis.surveyQuestionResults
                .filter(r => r.stratum6.indexOf(a.stratum3) > -1);
            subQuestion.ageAnalysis.surveyQuestionResults =
              subQuestion.ageAnalysis.surveyQuestionResults
                .filter(r => r.stratum6.indexOf(a.stratum3) > -1);
            subQuestion.countAnalysis.surveyQuestionResults.sort((a1, a2) => {
              if (a1.countValue > a2.countValue) {
                return -1;
              }
              if (a1.countValue < a2.countValue) {
                return 1;
              }
              return 0;
            });
            subQuestion.countAnalysis.surveyQuestionResults.push(
              this.addDidNotAnswerResult(
                subQuestion.countAnalysis.surveyQuestionResults, a.countValue)
            );
            for (const subResult of subQuestion.countAnalysis.surveyQuestionResults.
            filter(r => r.subQuestions === null)) {
              this.addMissingBiologicalSexResults(subQuestion.genderAnalysis,
                subQuestion.genderAnalysis.surveyQuestionResults.
                filter(r => r.stratum3 !== null && r.stratum3 === subResult.stratum3),
                a.countValue);
              this.addMissingAgeResults(subQuestion.ageAnalysis,
                subQuestion.ageAnalysis.surveyQuestionResults.
                filter(r => r.stratum3 !== null && r.stratum3 === subResult.stratum3),
                a.countValue);
            }
          }
          a.subQuestionFetchComplete = true;
        },
        error: err => {
          console.log('Error searching: ', err);
          this.loading = false;
        }
      });
  }

  public showSubAnswerGraphs(sqa: any, sq: any) {
    sqa.subExpanded = !sqa.subExpanded;
    if (!sqa.subQuestions) {
      this.showGraph(sqa);
    }
    if (sqa.subExpanded) {
      this.dbc.triggerEvent('conceptClick', 'View Graphs',
        'Expand to see graphs', this.surveyName + ' - Q'
        + sq.actualQuestionNumber + ' - ' + sq.conceptName + ' - ' + sqa.stratum4 +
        ' - ' + ' Icon', this.searchText.value, null);
      this.dbc.triggerEvent('conceptClick', 'View Graphs',
        'Expand to see graphs', this.surveyName + ' - Q'
        + sq.actualQuestionNumber + ' - ' + sq.conceptName + ' - ' + sqa.stratum4 +
        ' - ' + 'Sex Assigned at Birth', this.searchText.value, null);
    }
  }

  public hoverOnTooltip(q: any, event: string) {
    this.dbc.triggerEvent('tooltipsHover', 'Tooltips', 'Hover',
      this.surveyName + ' - Q' + q.actualQuestionNumber + ' - '
      + event, null, 'Survey Page Tooltip');
  }

  public isDidNotAnswer(sqa: any) {
    if (sqa.stratum4.toLowerCase() !== 'did not answer') {
      return false;
    }
    return true;
  }

  public getLabel(q: any, helpText: string) {
    return 'Family Medical History' + ' - Q' + q.actualQuestionNumber + ' - ' + helpText;
  }
}
