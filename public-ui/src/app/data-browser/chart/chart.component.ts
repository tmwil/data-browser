import {capitalize} from '@angular-devkit/core/src/utils/strings';
import { AfterViewInit, Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import * as highcharts from 'highcharts';

import { Analysis } from '../../../publicGenerated/model/analysis';
import { Concept } from '../../../publicGenerated/model/concept';
import { SurveyQuestionAnalysis } from '../../../publicGenerated/model/surveyQuestionAnalysis';
import { DbConfigService } from '../../utils/db-config.service';
import { DomainType } from '../../utils/enum-defs';

@Component({
  selector: 'app-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.css']
})
export class ChartComponent implements OnChanges, AfterViewInit {
  @Input() analysis: Analysis;
  @Input() analysis2: Analysis;
  @Input() surveyAnalysis: SurveyQuestionAnalysis;
  @Input() concepts: Concept[] = []; // Can put in analysis or concepts to chart. Don't put both
  @Input() selectedResult: any; // For ppi question, this is selected answer.
  @Input() pointWidth = 30;   // Optional width of bar or point or box plot
  @Input() backgroundColor = 'transparent'; // Optional background color
  @Input() chartTitle: string;
  @Input() conceptId: string;
  @Input() chartType: string;
  @Input() sources = false;
  @Input() genderId: string; // Hack until measurement design of graphs gender overlay
  @Input() domainType: DomainType;
  @Input() participantCount = 0;
  @Output() resultClicked = new EventEmitter<any>();
  @Input() domainCountAnalysis: any;
  @Input() surveyCountAnalysis: any;
  @Input() conceptName: string;
  @Input() test: string;
  chartOptions: any = null;
  constructor(private dbc: DbConfigService) {
    highcharts.setOptions({
      lang: { thousandsSep: ',' },
    });
  }
  // Render new chart on changes
  ngOnChanges(changes) {
    if ((this.analysis && this.analysis.results && this.analysis.results.length) ||
      (this.concepts && this.concepts.length) ||
      (this.surveyAnalysis && this.surveyAnalysis.surveyQuestionResults &&
        this.surveyAnalysis.surveyQuestionResults.length)) {
      // HC automatically redraws when changing chart options
      this.chartOptions = this.hcChartOptions();
    }
  }
  // renderChart after 1ms to fill container
  ngAfterViewInit() {
    setTimeout(() => {
      if (this.chartOptions) {
        this.chartOptions = this.hcChartOptions();
      }
    }, 1);
  }

  public doesNeedLegend() {
    return this.isGenderOrAgeAnalysis() ? true: false;
  }
  
  public isGenderOrAgeAnalysis() {
    return ((this.analysis &&
      (this.analysis.analysisId === this.dbc.GENDER_ANALYSIS_ID ||
        this.analysis.analysisId === this.dbc.AGE_ANALYSIS_ID)) ||
      (this.surveyAnalysis && (this.surveyAnalysis.analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID ||
        this.surveyAnalysis.analysisId === this.dbc.SURVEY_AGE_ANALYSIS_ID)));
  }

  public hcChartOptions(): any {
    const options = this.makeChartOptions();
    // Override title if they passed one
    if (this.chartTitle) {
      options.title.text = this.chartTitle;
    }
    const maxYAxis = options.series.length > 1 ?
      Math.max.apply(Math, options.series[1]['data'].map(function(o) { return o.y; })) :
      Math.max.apply(Math, options.series[0]['data'].map(function(o) { return o.y; }));
    return {
      chart: options.chart,
      lang: this.dbc.lang,
      credits: this.dbc.credits,
      title: '',
      subtitle: {},
      tooltip: {
        followPointer: true,
        outside: true,
        formatter: function(tooltip) {
          if (this.point.y <= 20 && this.point.toolTipHelpText.indexOf('%') === -1) {
            if (this.point.toolTipHelpText.length >= 100) {
              return '<div style="width: 500px; white-space: normal;">' +
                this.point.toolTipHelpText + '<b> &le; ' + this.point.y + '</b>' +
                '</div>';
            } else {
              return this.point.toolTipHelpText + '<b> &le; ' + this.point.y + '</b>';
            }
          } else if (this.point.toolTipHelpText.indexOf('%') >= 0) {
            if (this.point.medicalConceptCount === 20) {
              if (this.point.analysisId === 3102 || this.point.analysisId === 3101) {
                this.point.toolTipHelpText =
                  this.point.toolTipHelpText.replace('Medical Concept Count: <b>20</b>',
                    'Medical Concept Count: <b>&le; 20</b>');
              } else {
                this.point.toolTipHelpText =
                  this.point.toolTipHelpText.replace('Selected Answer: <b>20</b>',
                    'Selected Answer: <b>&le; 20</b>');
              }
            }
            if (this.point.totalDomainCount === 20) {
              if (this.point.analysisId === 3102 || this.point.analysisId === 3101) {
                this.point.toolTipHelpText =
                  this.point.toolTipHelpText.replace('Total With EHR Count: <b>20</b>',
                    'Total With EHR Count: <b>&le; 20</b>');
              } else {
                this.point.toolTipHelpText =
                  this.point.toolTipHelpText.replace(' Took Survey: <b>20</b>',
                    ' Took Survey: <b>&le; 20</b>');
              }
            }
            if (this.point.toolTipHelpText.length >= 100) {
              return '<div style="width: 500px; white-space: normal;">' +
                this.point.toolTipHelpText +
                '</div>';
            } else {
              return this.point.toolTipHelpText;
            }
            
          } else {
            if (this.point.toolTipHelpText.length >= 100) {
              return '<div style="width: 500px; white-space: normal;">' +
                this.point.toolTipHelpText + '</div>';
            } else {
              return this.point.toolTipHelpText;
            }
          }
        },
        useHTML: true,
        backgroundColor: '#f0f2f3',
        borderWidth: 0,
        shadow: false,
        enabled: true,
      },
      plotOptions: {
        series: {
          animation: {
            duration: 100,
          },
          pointWidth: this.pointWidthExists(options),
          minPointLength: 3,
          events: {
          },
        },
        pie: {
          borderColor: null,
          slicedOffset: 4,
          size: '100%',
          dataLabels: {
            enabled: true,
            style: this.dbc.DATA_LABEL_STYLE,
            distance: -50,
            formatter: function () {
              if (this.percentage < 1) {
                return this.point.name + ' ' + Number(this.percentage).toFixed(1) + '%';
              }
              return this.point.name + ' ' + Number(this.percentage).toFixed(0) + '%';
            }
          }
        },
        column: {
          shadow: false,
          borderColor: null,
          colorByPoint: true,
          groupPadding: this.setGroupPadding(),
          pointPadding: 0,
          borderWidth: 0,
          dataLabels: {
            enabled: false,
          },
          events: {},
        },
        bar: {
          shadow: false,
          borderColor: null,
          colorByPoint: true,
          groupPadding: 0,
          pointPadding: 0,
          dataLabels: {
            enabled: false,
          },
          events: {}
        }
      },
      yAxis: {
        title: {
          text: options.yAxisTitle ? options.yAxisTitle : null,
          style: {
            fontWeight: 'bold',
            textTransform: 'capitalize',
            whiteSpace: 'wrap',
            textOverflow: 'ellipsis',
            fontSize: '14px'
          }
        },
        min: options.yAxisMin != null ? options.yAxisMin : 20,
        max: maxYAxis,
        labels: ('dataOnlyLT20' in options.series[0] && options.series[0].dataOnlyLT20 != null
          && options.series[0].dataOnlyLT20) ?
         {
          style: {
            fontSize: '14px',
            whiteSpace: 'wrap',
            textOverflow: 'ellipsis'
          },
          formatter: function () {
            const label = this.axis.defaultLabelFormatter.call(this);
            // Change <= 20 count to display '<= 20'
            if (label <= 20) {
              return '&#8804; 20';
            }
            return label;
          },
          useHTML: true,
        } : {
            style: {
              fontSize: '12px',
              whiteSpace: 'wrap',
              textOverflow: 'ellipsis'
            },
            formatter: function () {
              const label = this.axis.defaultLabelFormatter.call(this);
              return label;
            },
            useHTML: true,
          },
        lineWidth: 1,
        lineColor: this.dbc.AXIS_LINE_COLOR,
        gridLineColor: this.backgroundColor
      },
      xAxis: {
        title: {
          text: options.xAxisTitle ? options.xAxisTitle : null,
          style: {
            fontWeight: 'bold',
            textTransform: 'capitalize',
            fontSize: '14px'
          },
        },
        categories: options.categories,
        // type: 'category',
        labels: {
          reserveSpace: true,
          style: {
            whiteSpace: 'wrap',
            textOverflow: 'ellipsis',
            width: '80px',
            fontSize: '14px'
          },
          formatter: function () {
            const label = this.axis.defaultLabelFormatter.call(this);
            // Change <= 20 count to display '<= 20'
            if (label.indexOf('>=') > -1) {
              return '&#8805; ' + label.replace('>=', '');
            }
            return label;
          },
          useHTML: true,
        },
        lineWidth: 1,
        lineColor: this.dbc.AXIS_LINE_COLOR,
        tickLength: 0
      },
      zAxis: {},
      legend: {
        enabled: this.doesNeedLegend(),
      },
      series: options.series,
    };
  }

  public makeChartOptions() {
    if (this.concepts.length > 0) {
      return this.makeConceptChartOptions();
    }
    if (this.analysis && this.analysis.analysisId === this.dbc.COUNT_ANALYSIS_ID) {
      return this.makeCountChartOptions(this.analysis.results, this.analysis.analysisName);
    }
    if (this.surveyAnalysis &&
      this.surveyAnalysis.analysisId === this.dbc.SURVEY_COUNT_ANALYSIS_ID) {
      return this.makeCountChartOptions(this.surveyAnalysis.surveyQuestionResults,
        this.surveyAnalysis.analysisName);
    }
    if (this.analysis &&
      this.analysis.analysisId === this.dbc.GENDER_ANALYSIS_ID) {
      return this.makeGenderChartOptions(this.analysis.results,
        'Sex Assigned at Birth', 'Sex Assigned at Birth', this.analysis.analysisId);
    }
    if (this.surveyAnalysis &&
      (this.surveyAnalysis.analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID)) {
      return this.makeGenderChartOptions(
        this.surveyAnalysis.surveyQuestionResults.filter(
          r => r.stratum4 === this.selectedResult.stratum4),
        this.surveyAnalysis.analysisName, this.selectedResult.stratum4, this.surveyAnalysis.analysisId);
    }
    /* Todo make charts for ethniticy and race
     * maybe cleanup / generalize pie chart
    if (
      this.analysis.analysisId === this.dbc.ETHNICITY_ANALYSIS_ID ||
      this.analysis.analysisId === this.dbc.RACE_ANALYSIS_ID) {
      return this.makePieChartOptions();
    }*/
    if (this.analysis && this.analysis.analysisId === this.dbc.AGE_ANALYSIS_ID) {
      return this.makeAgeChartOptions(
        this.analysis.results, 'Age at First Occurrence in Participant Record',
        this.analysis.analysisName,
        'stratum2', this.analysis.analysisId);
    }
    if (this.surveyAnalysis &&
      (this.surveyAnalysis.analysisId === this.dbc.SURVEY_AGE_ANALYSIS_ID)) {
      return this.makeAgeChartOptions(
        this.surveyAnalysis.surveyQuestionResults.filter(
          r => r.stratum4 === this.selectedResult.stratum4),
        'Age When Survey Was Taken',
        this.selectedResult.stratum4, 'stratum5', this.surveyAnalysis.analysisId);
    }
    if (this.analysis &&
      this.analysis.analysisId === this.dbc.MEASUREMENT_VALUE_ANALYSIS_ID) {
      if (this.isPregnancyOrWheelChair()) {
        return this.makeStackedChartOptions(this.analysis.analysisName);
      }
      return this.makeMeasurementChartOptions();
    }
    console.log('Error: Can not make chart options for this analysis. :', this.analysis);
  }
  seriesClick(event) {
    // Todo handle click and log events in analytics
    // console.log('Global series clicked ', this.analysis, 'Clicked analysis', event.point);
  }

  public makeCountChartOptions(results: any, analysisName: string) {
    let data = [];
    let cats = [];
    for (const a of results) {
      data.push({ name: a.stratum4, y: a.countValue, thisCtrl: this, result: a, toolTipHelpText:
        '<b>' + a.analysisStratumName + '</b>'});
      cats.push(a.stratum4);
    }
    data = data.sort((a, b) => {
        if (a.name > b.name) {
          return 1;
        }
        if (a.name < b.name) {
          return -1;
        }
        return 0;
      }
    );
    cats = cats.sort((a, b) => {
      if (a > b) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
      return 0;
    });
    const seriesClick = event => {
      const thisCtrl = event.point.options.thisCtrl;
      // Todo handle click and log events in analytics
      // console.log('Count plot Clicked point :', event.point);
      thisCtrl.resultClicked.emit(event.point.result);
    };
    // Override tooltip and colors and such
    const series = {
      name: this.analysis.analysisName,
      colorByPoint: true,
      data: data,
      dataOnlyLT20: false,
      colors: [this.dbc.COLUMN_COLOR],
      events: {
        click: seriesClick
      }
    };
    return {
      chart: { type: 'column' },
      title: { text: null },
      series: [series],
      categories: cats,
      xAxis: {
        labels: {
          style: {
            align: 'right',
            fontSize: '14px'
          }
        }
      },
      pointWidth: this.pointWidth,
      xAxisTitle: this.analysis.analysisName,
      yAxisTitle: null,
      tooltip: { pointFormat: '{point.y}' },
      yAxisMin: null
    };
  }

  public makeConceptChartOptions() {
    const data = [];
    const cats = [];
    // Sort by count value
    this.concepts = this.concepts.sort((a, b) => {
        if (this.sources) {
          if (a.sourceCountValue < b.sourceCountValue) {
            return 1;
          }
          if (a.sourceCountValue > b.sourceCountValue) {
            return -1;
          }
          return 0;
        } else {
          if (a.countValue < b.countValue) {
            return 1;
          }
          if (a.countValue > b.countValue) {
            return -1;
          }
          return 0;
        }
      }
    );
    for (const a of this.concepts) {
      let toolTipText = '';
      if (!this.sources) {
        if (a.countValue > 20 ) {
          toolTipText = a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ' +
            '<br/>' + 'Participant Count: ' + '<b>' + a.countValue + '</b>';
        } else {
          toolTipText = a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ' +
            '<br/>' + 'Participant Count: ';
        }
        data.push({
          name: a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ',
          y: a.countValue,
          color: this.dbc.COLUMN_COLOR,
          toolTipHelpText: toolTipText
        });
        cats.push(a.conceptName);
      } else {
        if (a.sourceCountValue > 20 ) {
          toolTipText = a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ' +
            '<br/>' + 'Participant Count: ' + '<b>' + a.sourceCountValue + '</b>';
        } else {
          toolTipText = a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ' +
            '<br/>' + 'Participant Count: ';
        }
        data.push({
          name: a.conceptName + ' (' + a.vocabularyId + '-' + a.conceptCode + ') ',
          y: a.sourceCountValue,
          color: this.dbc.COLUMN_COLOR,
          toolTipHelpText: toolTipText
        });
        cats.push(a.vocabularyId + '-' + a.conceptCode);
      }
    }
    const temp = data.filter(x => x.y > 20);
    const dataOnlyLT20 = temp.length > 0 ? false : true;
    // Override tooltip and colors and such
    const series = {
      name: this.concepts[0].domainId, colorByPoint: true, data: data, colors: ['#6CAEE3'],
      dataOnlyLT20: dataOnlyLT20
    };
    return {
      chart: {
        type: this.sources ? 'column' : 'bar',
        backgroundColor: this.backgroundColor,
        tooltip: {
          headerFormat: '<span>{point.key} <br/>',
          pointFormat: '{point.y}</span>'
        },
      },
      title: { text: null, style: this.dbc.CHART_TITLE_STYLE },
      series: [series],
      categories: cats,
      pointPadding: 0.25,
      minPointLength: 3,
      pointWidth: 20,
      xAxisTitle: this.sources ? 'Source Concepts' : 'Top concepts',
      yAxisTitle: 'Participant Count',
      yAxisMin: temp.length > 0 ? 0 : 20,
    };
  }

  public makeGenderChartOptions(results: any, analysisName: string,
                                seriesName: string, analysisId: number) {
    let data = [];
    let cats = [];
    let totalData = [];
    let legendText = null;
    let totalLegendText = null;
    const yAxisLabel = null;
    // LOOP CREATES DYNAMIC CHART VARS
    for (const a of results) {
      // For normal Gender Analysis , the stratum2 is the gender . For ppi it is stratum5;
      let analysisStratumName = null;
      let toolTipHelpText = null;
      let totalToolTipHelpText = null;
      let bsResult = null;
      let color = null;
      if (analysisId === this.dbc.GENDER_ANALYSIS_ID) {
        color = this.dbc.COLUMN_COLOR;
        legendText = seriesName + ', Medical Concept';
        totalLegendText = seriesName + ', Total With EHR';
        analysisStratumName = a.analysisStratumName;
        if (analysisStratumName === null) {
          analysisStratumName = this.dbc.GENDER_STRATUM_MAP[a.stratum2];
        }
        if (a.countValue <= 20) {
          toolTipHelpText = 'Sex Assigned at Birth: ' + '<b>' + analysisStratumName +
            '</b>' + '<br/> Participant Count: ';
        } else {
          toolTipHelpText = 'Sex Assigned at Birth: ' + '<b>' + analysisStratumName + '</b>' +
            '<br/> Participant Count: ' + '<b>' + a.countValue + '</b>';
        }
        bsResult = this.domainCountAnalysis.genderCountAnalysis.results.
        filter(x => x.stratum4 === a.stratum2)[0];
        totalToolTipHelpText = null;
        if (bsResult.countValue > 20) {
          totalToolTipHelpText = 'Sex Assigned at Birth: ' + '<b>' + analysisStratumName +
            '</b>' + '<br/> Medical Concept Count: ' + '<b>' + a.countValue + '</b>' +
            '<br/> Total With EHR Count: ' + '<b>' + bsResult.countValue + '</b>' +
            '<br/> % with Medical Concept in EHR: <b>' + ((a.countValue/bsResult.countValue)*100).toFixed() + '</b> %';
        } else {
          const percentage = Number(((a.countValue/bsResult.countValue)*100).toFixed());
          totalToolTipHelpText = 'Sex Assigned at Birth: ' + '<b>' + analysisStratumName + '</b>' +
            '</b>' + '<br/> Medical Concept Count: ' + '<b>' + a.countValue + '</b>' +
            '<br/> Total With EHR Count: ' + '<b>' + bsResult.countValue + '</b>' +
            '<br/> % with Medical Concept in EHR: <b>' + ((percentage >= 0) ? percentage : 100) + '</b> %';
        }
      } else if (analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID) {
        color = this.dbc.COLUMN_COLOR;
        analysisStratumName = a.analysisStratumName;
        if (analysisStratumName === null) {
          analysisStratumName = this.dbc.GENDER_STRATUM_MAP[a.stratum5];
        }
        legendText = 'Sex Assigned At Birth' + ', Selected Answered Count';
        totalLegendText = 'Sex Assigned At Birth' + ', Took Survey Count';
        if (a.countValue > 20) {
          toolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) + '</b> <br/> ' +
            'Sex Assigned at Birth: ' + '<b>' + analysisStratumName + '</b>' +
            '<br/> Participant Count: ' + '<b>' + a.countValue + '</b>';
        } else {
          toolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) + '</b> <br/> ' +
            'Sex Assigned at Birth: ' + '<b>' + analysisStratumName + '</b>' +
            '<br/> Participant Count: ';
        }
        bsResult = this.surveyCountAnalysis.genderCountAnalysis.results.
        filter(x => x.stratum2 === a.stratum5)[0];
        if (bsResult.countValue > 20) {
          totalToolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) + '</b>' + '<br/>' + analysisStratumName +
            ', Selected Answer: ' + '<b>' + a.countValue + '</b>' + '<br/>' + analysisStratumName +
            ', Took Survey: ' + '<b>' + bsResult.countValue + '</b>' +
            '<br/> % Answered: <b>' + ((a.countValue/bsResult.countValue)*100).toFixed() + '</b> %';
        } else {
          const percentage = Number(((a.countValue/bsResult.countValue)*100).toFixed());
          totalToolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) + '</b>' + '<br/>' + analysisStratumName +
            ', Selected Answer: ' + '<b>' + a.countValue + '</b>' + '<br/>' + analysisStratumName +
            ', Took Survey: ' + '<b>' + bsResult.countValue + '</b>' +
            '<br/> % Answered: <b>' + ((percentage >= 0) ? percentage : 100) + '</b> %';
        }
      }
      data.push({
        name: a.analysisStratumName
        , y: a.countValue, color: color, sliced: true,
        toolTipHelpText: toolTipHelpText,
      });
      totalData.push({
        name: bsResult.analysisStratumName
        , y: bsResult.countValue, color: this.dbc.TOTAL_COLUMN_COLOR, sliced: true,
        toolTipHelpText: totalToolTipHelpText, medicalConceptCount: a.countValue, totalDomainCount: bsResult.countValue,
        analysisId: analysisId
      });
      cats.push(a.analysisStratumName);
    }
    data = data.sort((a, b) => {
        if (a.name > b.name) {
          return 1;
        }
        if (a.name < b.name) {
          return -1;
        }
        return 0;
      }
    );
    totalData = totalData.sort((a, b) => {
        if (a.name > b.name) {
          return 1;
        }
        if (a.name < b.name) {
          return -1;
        }
        return 0;
      }
    );
    cats = cats.sort((a, b) => {
      if (a > b) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
      return 0;
    });
    const temp = data.filter(x => x.y > 20);
    const dataOnlyLT20 = temp.length > 0 ? false : true;
    const series = [
      {
        color: '#2691D0',
        legendColor: '#2691D0',
        name: legendText, colorByPoint: false, data: data, dataOnlyLT20: dataOnlyLT20
      },
      {
        color: '#262262',
        legendColor: '#262262',
        name: totalLegendText, colorByPoint: false,
        data: totalData, dataOnlyLT20: dataOnlyLT20
      }];
    return {
      chart: { type: 'column', backgroundColor: 'transparent' },
      title: { text: analysisName, style: this.dbc.CHART_TITLE_STYLE },
      series: series,
      categories: cats,
      color: this.dbc.COLUMN_COLOR,
      xAxisTitle: analysisName,
      yAxisTitle: yAxisLabel !== null ? yAxisLabel : 'Participant Count',
      tooltip: {
        headerFormat: '<span> ',
        pointFormat: '{point.y} {point.name}</span>',
      },
      yAxisMin: temp.length > 0 ? 0 : 20
    };
  }

  public makeAgeChartOptions(results: any, analysisName: string,
                             seriesName: string, ageDecileStratum: string, analysisId: number) {
    let yAxisLabel = null;
    let legendText = null;
    let totalLegendText = null;
    // Age results have two stratum-- 1 is concept, 2 is age decile
    // Sort by age decile (stratum2 or stratum5)
    if (this.domainType === 'physical measurements') {
      seriesName = 'Age When Physical Measurement Was Taken';
    } else if (this.domainType === 'ehr') {
      seriesName = 'Age at First Occurrence in Participant Record';
    }
    results = results.sort((a, b) => {
        const anum = Number(a[ageDecileStratum]);
        const bnum = Number(b[ageDecileStratum]);
        if (anum > bnum) {
          return 1;
        }
        if (anum < bnum) {
          return -1;
        }
        return 0;
      }
    );
    const data = [];
    const cats = [];
    const totalData = [];
    const color = this.dbc.COLUMN_COLOR;
    let ageHelpText = null;
    for (const a of results) {
      let toolTipHelpText = null;
      let ageResult = null;
      let totalToolTipHelpText = null;
      if (analysisId === this.dbc.AGE_ANALYSIS_ID) {
        ageHelpText = seriesName;
        legendText = seriesName + ', Medical Concept';
        totalLegendText = seriesName + ', Total With EHR';
        if (a.countValue > 20 ) {
          toolTipHelpText = ageHelpText + ' : ' +
            '<b>' +  a.analysisStratumName + '</b>' + '<br/> Participant Count: ' +
            '<b>' + a.countValue + '</b>';
        } else {
          toolTipHelpText = ageHelpText + ' : ' +
            '<b>' +  a.analysisStratumName + '</b>' + '<br/> Participant Count: ';
        }
        ageResult = this.domainCountAnalysis.ageCountAnalysis.results.
        filter(x => x.stratum4 === a.stratum2)[0];
        if (ageResult.countValue > 20) {
          totalToolTipHelpText = ageHelpText + ': ' + '<b>' + a.analysisStratumName +
            '</b>' + '<br/> Medical Concept Count: ' + '<b>' + a.countValue + '</b>' +
            '<br/> Total With EHR Count: ' + '<b>' + ageResult.countValue + '</b>' +
            '<br/> % with Medical Concept in EHR: <b>' + ((a.countValue/ageResult.countValue)*100).toFixed() + '</b> %';
        } else {
          const percentage = Number(((a.countValue/ageResult.countValue)*100).toFixed());
          totalToolTipHelpText = ageHelpText + ': '  + '<b>' + a.analysisStratumName + '</b>' +
            '</b>' + '<br/> Medical Concept Count: ' + '<b>' + a.countValue + '</b>' +
            '<br/> Total With EHR Count: ' + '<b>' + ageResult.countValue + '</b>' +
            '<br/> % with Medical Concept in EHR: <b>' + ((percentage >= 0) ? percentage : 100) + '</b> %';
        }
      } else if (analysisId === this.dbc.SURVEY_AGE_ANALYSIS_ID) {
        ageHelpText = 'Age When Survey Was Taken';
        legendText = ageHelpText + ', Selected Answered Count';
        totalLegendText = ageHelpText + ', Took Survey Count';
        if (a.countValue > 20 ) {
          toolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) +
            '</b> <br/> ' + ageHelpText + ' : ' +
            '<b> ' +  a.analysisStratumName + ' </b>' +
            '<br/>' + 'Participant Count: ' + '<b>' +  a.countValue + '</b>';
        } else {
          toolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) +
            '</b> <br/> ' + ageHelpText + ' : ' +
            '<b> ' +  a.analysisStratumName + ' </b>' +
            '<br/>' + 'Participant Count: ';
        }
        ageResult = this.surveyCountAnalysis.ageCountAnalysis.results.
        filter(x => x.stratum2 === a.stratum5)[0];
        if (ageResult.countValue > 20) {
          totalToolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) + '</b>' + '<br/>' + a.analysisStratumName +
            ', Selected Answer: ' + '<b>' + a.countValue + '</b>' + '<br/>' + a.analysisStratumName +
            ', Took Survey: ' + '<b>' + ageResult.countValue + '</b>' +
            '<br/> % Answered: <b>' + ((a.countValue/ageResult.countValue)*100).toFixed() + '</b> %';
        } else {
          const percentage = Number(((a.countValue/ageResult.countValue)*100).toFixed());
          totalToolTipHelpText = '<b>' + this.getSurveyAnswerText(a.stratum4) + '</b>' + '<br/>' + a.analysisStratumName +
            ', Selected Answer: ' + '<b>' + a.countValue + '</b>' + '<br/>' + a.analysisStratumName +
            ', Took Survey: ' + '<b>' + ageResult.countValue + '</b>' +
            '<br/> % Answered: <b>' + ((percentage >= 0) ? percentage : 100) + '</b> %';
        }
      }
      data.push({
        name: a.analysisStratumName,
        y: a.countValue, color: color,
        toolTipHelpText: toolTipHelpText,
      });
      totalData.push({
        name: ageResult.analysisStratumName
        , y: ageResult.countValue, color: this.dbc.TOTAL_COLUMN_COLOR, sliced: true, medicalConceptCount: a.countValue,
        totalDomainCount: ageResult.countValue,
        toolTipHelpText: totalToolTipHelpText, analysisId: analysisId
      });
      cats.push(a.analysisStratumName);
    }
    const temp = data.filter(x => x.y > 20);
    const dataOnlyLT20 = temp.length > 0 ? false : true;
    const series = [
      {
        color: '#2691D0',
        legendColor: '#2691D0',
        name: legendText, colorByPoint: false, data: data, dataOnlyLT20: dataOnlyLT20
      },
      {
        color: '#262262',
        legendColor: '#262262',
        name: totalLegendText, colorByPoint: false,
        data: totalData, dataOnlyLT20: dataOnlyLT20
      }];
    return {
      chart: { type: 'column', backgroundColor: 'transparent' },
      title: {
        text: this.getChartTitle(this.domainType),
        style: this.dbc.CHART_TITLE_STYLE
      },
      color: this.dbc.COLUMN_COLOR,
      series: series,
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: this.domainType === 'physical measurements' ? seriesName : analysisName,
      yAxisTitle: yAxisLabel !== null ? yAxisLabel : 'Participant Count',
      tooltip: {
        headerFormat: '<span> ',
        pointFormat: '{point.name}<br/ > {point.y}</span>'
      },
      yAxisMin: temp.length > 0 ? 0 : 20
    };
  }

  // Histogram data analyses come already binned
  // The value is in stratum 4, the unit in stratum5, the countValue in the bin is countValue
  // and we also have
  // sourceCountValue
  public makeMeasurementChartOptions() {
    let data = [];
    const cats = [];
    // Todo overlay genders on one graph , use hack for separate gender graphs now
    // Hack to filter gender
    let results = this.analysis.results.concat([]);
    if (this.genderId) {
      results = results.filter(r => r.stratum3 === this.genderId);
    }
    for (const a of results) {
      let analysisStratumName = a.analysisStratumName;
      if (analysisStratumName === null) {
        analysisStratumName = this.dbc.GENDER_STRATUM_MAP[a.stratum3];
      }
      let tooltipText = '';
      if (a.stratum2 !== 'No unit') {
        if (a.countValue <= 20) {
          tooltipText = '<b>' + analysisStratumName + '</b>' +
            '<br/>' + 'Measurement Value / Range:';
          if (a.stratum4.indexOf('>=') > -1) {
            tooltipText = tooltipText + ' &ge; <b>' + a.stratum4.replace('>=', '')
              + '</b> <br/>' + 'Participant Count: ';
          } else {
            tooltipText = tooltipText + ' <b>' + a.stratum4
              + '</b> <br/>' + 'Participant Count: ';
          }
        } else {
          tooltipText = '<b>' + analysisStratumName + '</b>' +
            '<br/>' + 'Measurement Value / Range:';
          if (a.stratum4.indexOf('>=') > -1) {
            tooltipText = tooltipText + ' &ge; <b>' + a.stratum4.replace('>=', '')
              + '</b> <br/>' + 'Participant Count: ' +
              '<b>' + a.countValue + '</b>';
          } else {
            tooltipText = tooltipText + ' <b>' + a.stratum4
              + '</b> <br/>' + 'Participant Count: ' +
              '<b>' + a.countValue + '</b>';
          }
        }
      } else {
        if (a.countValue <= 20) {
          tooltipText = '<b>' + analysisStratumName + '</b>' +
            '<br/>' + 'Measurement Value : <b>' + a.stratum4
            + '</b> <br/>' + 'Participant Count: ';
        } else {
          tooltipText = '<b>' + analysisStratumName + '</b>' +
            '<br/>' + 'Measurement Value : <b>' + a.stratum4
            + '</b> <br/>' + 'Participant Count: ' +
            '<b>' + a.countValue + '</b>';
        }
      }
      data.push({ name: a.stratum4, y: a.countValue, thisCtrl: this,
        result: a, toolTipHelpText: tooltipText, binWidth: a.stratum6});
    }
    const lessThanData = data.filter(
      d => d.name != null && d.name.indexOf('< ') >= 0);
    const greaterThanData = data.filter(
      d => d.name != null && d.name.indexOf('>= ') >= 0);
    data = data.filter(
      d => d.name != null && d.name.indexOf('< ') === -1);
    data = data.filter(
      d => d.name != null && d.name.indexOf('>= ') === -1);
    data = data.sort((a, b) => {
      let aVal: any = a.name;
      let bVal: any = b.name;
      // Sort  numeric data as number
      if (a.name.indexOf(' - ') > 0) {
        aVal = a.name.split(' - ')[1];
      } else if (a.name.indexOf('< ') >= 0) {
        aVal = a.name.replace('< ', '');
      } else if (a.name.indexOf('>= ') >= 0) {
        aVal = a.name.replace('>= ', '');
      }
      if (b.name.indexOf(' - ') > 0) {
        bVal = b.name.split(' - ')[1];
      } else if (b.name.indexOf('< ') >= 0) {
        bVal = b.name.replace('< ', '');
      } else if (b.name.indexOf('>= ') >= 0) {
        bVal = b.name.replace('>= ', '');
      }
      if (isNaN(Number(aVal))) {
        // Don't do anything
      } else {
        // Make a number so sort works
        aVal = Number(aVal);
        bVal = Number(bVal);
      }
      if (aVal > bVal) {
        return -1;
      }
      if (aVal < bVal) {
        return 1;
      }
      return 0;
    });
    if (lessThanData.length > 0 && greaterThanData.length > 0) {
      data.unshift(greaterThanData[0]);
      data.push(lessThanData[0]);
    } else if (lessThanData.length > 0) {
      data.push(lessThanData[0]);
    } else if (greaterThanData.length > 0 ) {
      data.unshift(greaterThanData[0]);
    }
    if (data.length > 2) {
      if (greaterThanData.length === 0) {
        if (isNaN(Number(data[0].name))) {
          // Don't do anything
        } else {
          data[0].name = '>= ' + data[0].name;
        }
      }
      if (lessThanData.length === 0) {
        if (isNaN(Number(data[data.length - 1].name))) {
          // Don't do anything
        } else {
          data[data.length - 1].name = '< ' + data[data.length - 1].name;
        }
      }
    }
    for (const d of data) {
      cats.push(d.name);
    }
    // Todo we will use this later in drill downs and such
    const seriesClick = event => {
      const thisCtrl = event.point.options.thisCtrl;
      // Todo handle click events
      // console.log('Histogram plot Clicked point :',  event.point);
      // thisCtrl.resultClicked.emit(event.point.result);
    };
    // Unit for measurements is in stratum5
    if (this.analysis.unitName === 'cm') {
      this.analysis.unitName = 'centimeter';
    }
    const unit = this.analysis.unitName ? this.analysis.unitName : '';
    const temp = data.filter(x => x.y > 20);
    const dataOnlyLT20 = temp.length > 0 ? false : true;
    const series: any = {
      name: this.analysis.analysisName,
      colorByPoint: true,
      data: data,
      dataOnlyLT20: dataOnlyLT20,
      colors: [this.dbc.GENDER_PM_COLOR],
    };
    // Note that our data is binned already so we use a column chart to show histogram
    // however we need to style it to make it look like a histogram. Some measurements
    // like pregnancy and wheel chair we don't want a histogram.
    if (this.chartType === 'histogram') {
      // Make column chart look like  a histogram with these options
      series.pointPadding = 0;
      series.borderWidth = 0;
      series.groupPadding = 0;
      series.pointWidth = data.length >= 15 ? 15 : 18;
      series.shadow = false;
    }
    return {
      chart: { type: 'bar', backgroundColor: this.backgroundColor },
      title: { text: this.chartTitle },
      series: [series],
      categories: cats,
      pointWidth: this.pointWidth,
      xAxisTitle: unit,
      yAxisTitle: 'Participant Count',
      tooltip: {
        headerFormat: '{point.key} ' + unit + '<br/>',
        pointFormat: '{point.y} participants'
      },
      yAxisMin: temp.length > 0 ? 0 : 20,
    };
  }

  public getChartTitle(domainType: string) {
    if (domainType === DomainType.EHR) {
      return 'Age at First Occurrence in EHR.';
    } else if (domainType === DomainType.SURVEYS) {
      return 'Age When Survey Was Taken';
    } else if (domainType === DomainType.PHYSICAL_MEASUREMENTS) {
      return 'Age When Physical Measurement Was Taken';
    }
  }

  public makeStackedChartOptions(seriesName: string) {
    const data = [];
    const cats = [];
    const color = this.dbc.COLUMN_COLOR;
    const order = ['8507', '8532', '0'];
    this.analysis.results.sort(function(a, b) {
      return order.indexOf(a.stratum3) - order.indexOf(b.stratum3);
    });
    for (const a of this.analysis.results) {
      let toolTipText = '';
      if (a.countValue > 20 ) {
        toolTipText =  'Sex Assigned At Birth: ' + '<b>' + a.analysisStratumName + '</b>' +
          '<br/>' + 'Participant Count: ' + '<b>' + a.countValue + '</b>';
      } else {
        toolTipText = 'Sex Assigned At Birth: ' + '<b>' + a.analysisStratumName + '</b>' +
          '<br/>' + 'Participant Count: ';
      }
      data.push({
        name: a.stratum4,
        y: a.countValue, color: color,
        toolTipHelpText: toolTipText,
      });
      cats.push(a.analysisStratumName);
    }
    const temp = data.filter(x => x.y > 20);
    const dataOnlyLT20 = temp.length > 20 ? false : true;
    const series = {
      name: seriesName,
      colorByPoint: true,
      data: data,
      dataOnlyLT20: dataOnlyLT20
    };
    return {
      chart: { type: 'column', backgroundColor: this.backgroundColor },
      title: { text: this.chartTitle },
      series: [series],
      categories: cats,
      xAxis: {
        categories: cats,
      },
      pointWidth: this.pointWidth,
      xAxisTitle: '',
      yAxisTitle: 'Participant Count',
      yAxisMin: temp.length > 0 ? 0 : 20,
    };
  }

  public isPregnancyOrWheelChair() {
    if (['903111', '903120'].indexOf(this.conceptId) > -1) {
      return true;
    }
    return false;
  }

  public getNumDecimals (value: any) {
    if ((value % 1) !== 0) {
      return value.toString().split('.')[1].length;
    }
    return 0;
  }

  public getSurveyAnswerText(answer: string) {
    if (answer.includes(':')) {
      const answer_split = answer.split(':');
      let result = '';
      for (let i = 0; i < answer_split.length - 1; i++) {
        result += answer_split[i];
      }
      result += '<b>' + answer_split[answer_split.length - 1] + '</b>';
      return result;
    }
    return '<b>' + answer + '</b>';
  }
  
  public pointWidthExists(options: any) {
    if ("pointWidth" in options) {
      return options.pointWidth;
    }
    return null;
  }
  
  public setGroupPadding() {
    if (this.analysis && this.analysis.analysisId === this.dbc.GENDER_ANALYSIS_ID) {
      return 0.35;
    } else if (this.surveyAnalysis && this.surveyAnalysis.analysisId === this.dbc.SURVEY_GENDER_ANALYSIS_ID) {
      return 0.40;
    } else if (this.analysis && this.analysis.analysisId === this.dbc.AGE_ANALYSIS_ID) {
      return 0.2;
    } else if (this.surveyAnalysis && this.surveyAnalysis.analysisId === this.dbc.SURVEY_AGE_ANALYSIS_ID) {
      return 0.25;
    }
  }
}
