import {Component, Input, OnChanges, OnDestroy} from '@angular/core';

@Component({
  selector: 'app-highlight-search',
  templateUrl: './highlight-search.component.html',
  styleUrls: ['./highlight-search.component.css']
})
export class HighlightSearchComponent implements OnChanges, OnDestroy {
  @Input() text: string;
  @Input() searchTerm: string;
  @Input() isSource: boolean;
  @Input() conceptId: string;
  @Input() isStandard: string;
  @Input() indNum: number;
  words: string[] = [];
  matchString: RegExp;
  ngOnChanges() {
    if (!this.searchTerm || this.searchTerm === ' ' ||
      this.searchTerm === '.' || this.searchTerm === ',') {
      this.words = [this.text];
    } else {
      let searchWords = this.searchTerm.split(new RegExp(',| '));
      searchWords = searchWords.filter(w => w.length > 0 );
      searchWords = searchWords.map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
      this.matchString = new RegExp(searchWords.join('|'));
      const matches = this.text.match(new RegExp(this.matchString, 'gi'));
      const splits = this.text.split(new RegExp(this.matchString, 'i'));
      if (matches) {
        this.words = [];
        for (let i = 0; i < matches.length; i++) {
          this.words.push(splits[i], matches[i]);
        }
        if (splits.length > matches.length) {
          this.words.push(splits[splits.length - 1]);
        }
      } else {
        this.words = [this.text];
      }
    }
  }
  ngOnDestroy() {
    this.words = [];
  }

  public searchTermCheck(searchTerm: string) {
    if (searchTerm && searchTerm !== ' ' && searchTerm !== ',' && searchTerm !== '.') {
      return true;
    }
    return false;
  }
}
