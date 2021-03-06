import { Component, OnInit } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HeaderFooterService } from '../../services/header-footer.service';

@Component({
  selector: 'app-rh-footer',
  templateUrl: './rh-footer.component.html',
  styleUrls: ['./rh-footer.component.css', '../../../styles/template.css']
})
export class RhFooterComponent implements OnInit {
  menuItems: any;
  workBenchIsBeta: boolean;
  allOfUsUrl: any;
  constructor(public hFService: HeaderFooterService) { }

  ngOnInit() {
  this.menuItems = this.hFService.menu;
  this.workBenchIsBeta = environment.workBenchIsBeta;
  this.allOfUsUrl = environment.researchAllOfUsUrl;
  }

}
