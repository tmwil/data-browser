import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule } from '@angular/router';
import { LocalStorageModule } from 'angular-2-local-storage';
import { NgxPaginationModule } from 'ngx-pagination';

//  components
import { BetaBarComponent } from '../components/beta-bar/beta-bar.component';
import { IterateOverObjPipe } from '../utils/iterate-over-obj.pipe';
import { EmergencyComponent } from '../views/emergency/emergency.component';
import { LoginComponent } from '../views/login/login.component';
import { PageTemplateSignedOutComponent } from '../views/page-template-signed-out/page-template-signed-out.component';
import { BreadcrumbComponent } from './components/breadcrumb/breadcrumb.component';
import { HighlightSearchComponent } from './components/highlight-search/highlight-search.component';
import { ItemSelectComponent } from './components/item-select/item-select.component';
import { ItemComponent } from './components/item-select/item/item.component';
import { RhHeaderComponent } from './components/rh-header/rh-header.component';
import { SpinnerComponent } from './components/spinner/spinner.component';

@NgModule({
  imports: [
    RouterModule,
    CommonModule,
    FormsModule,
    HttpModule,
    ReactiveFormsModule,
    NgxPaginationModule,
    LocalStorageModule
  ],
  declarations: [
    HighlightSearchComponent,
    BreadcrumbComponent,
    EmergencyComponent,
    LoginComponent,
    PageTemplateSignedOutComponent,
    RhHeaderComponent,
    BetaBarComponent,
    SpinnerComponent,
    ItemSelectComponent,
    ItemComponent,
    IterateOverObjPipe
  ],
  exports: [
    CommonModule,
    FormsModule,
    HttpModule,
    ReactiveFormsModule,
    NgxPaginationModule,
    HighlightSearchComponent,
    BreadcrumbComponent,
    EmergencyComponent,
    LoginComponent,
    PageTemplateSignedOutComponent,
    RhHeaderComponent,
    BetaBarComponent,
    SpinnerComponent,
    LocalStorageModule,
    ItemSelectComponent,
    IterateOverObjPipe
  ]
})
export class SharedModule { }
