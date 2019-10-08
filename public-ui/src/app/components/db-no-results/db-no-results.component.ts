import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import {DataBrowserService} from 'publicGenerated';
import { ISubscription } from 'rxjs/Subscription';
import { DbConfigService } from '../../utils/db-config.service';

@Component({
  selector: 'app-db-no-results',
  templateUrl: './db-no-results.component.html',
  styleUrls: ['./db-no-results.component.css', '../../styles/template.css']
})
export class DbNoResultsComponent implements OnChanges, OnDestroy {
  @Input() searchText;
  @Output() newDomain: EventEmitter<any> = new EventEmitter();
  results;
  loading;
  pmResults: any = [];
  prevSearchText: string;
  private subscriptions: ISubscription[] = [];
  constructor(
    private api: DataBrowserService,
    private router: Router,
    private dbc: DbConfigService) {
  }

  ngOnChanges() {
    this.prevSearchText = localStorage.getItem('searchText');
    if (!this.prevSearchText) {
      this.prevSearchText = '';
    } else {
      this.searchText.setValue(this.prevSearchText);
    }
    if (this.searchText) {
      this.loading = true;
      this.searchDomains(this.searchText.value);
    }
  }

  ngOnDestroy() {
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  public goToResult(r) {
    if (this.results && this.results.domainInfos) {
      this.results.domainInfos.forEach(domain => {
        if (r.domain === domain.domain) {
          const payload = {
            domain: r,
            searchText: this.searchText.value
          };
          localStorage.setItem('ehrDomain', JSON.stringify(r));
          this.newDomain.emit(payload);
          this.router.navigate(
            ['ehr/' + this.dbc.domainToRoute[r.domain.toLowerCase()]],
            {
              queryParams: { search: this.searchText.value, fromDifferentDomain: true }
            });
        }
      });
    }
    if (this.results && this.results.surveyModules) {
      this.results.surveyModules.forEach(survey => {
        if (r.conceptId === survey.conceptId) {
          localStorage.setItem('surveyModule', JSON.stringify(r));
          localStorage.setItem('searchText', this.searchText.value);
          this.dbc.conceptIdNames.forEach(idName => {
            if (r.conceptId === idName.conceptId) {
              const payload = {
                domain: r,
                searchText: this.searchText.value
              };
              localStorage.setItem('surveyModule', JSON.stringify(r));
              this.newDomain.emit(payload);
              this.router.navigateByUrl('survey/' + idName.conceptName);
            }
          });
        }
      });
    }
  }

  public searchDomains(query: string) {
    this.subscriptions.push(this.api.getDomainSearchResults(query)
      .subscribe(results => {
        this.results = results;
        this.pmResults = results.domainInfos.filter(d => d.name === 'Physical Measurements');
        this.pmResults = this.pmResults.filter(d => d.standardConceptCount > 0);
        this.loading = false;
      }));
  }

  public goToPMResult(r) {
    this.router.navigateByUrl(
      'physical-measurements/' + '/' + this.searchText.value
    );
  }

}
