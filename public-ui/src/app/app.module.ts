import {ErrorHandler, NgModule} from '@angular/core';

import {Http} from '@angular/http';

import {environment} from 'environments/environment';

import * as StackTrace from 'stacktrace-js';
import {ErrorReporterService} from './services/error-reporter.service';
import {AppComponent, overriddenUrlKey} from './views/app/app.component';

/* Our Modules */
import {
  ApiModule,
  Configuration
} from 'publicGenerated';


import { DataBrowserModule } from './data-browser/data-browser.module';
import { ServerConfigService } from './services/server-config.service';
import { SignInService } from './services/sign-in.service';
import { SharedModule } from './shared/shared.module';
// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

import {ConfigService, DataBrowserService} from 'publicGenerated';
import { DbConfigService } from './utils/db-config.service';
import { TooltipService } from './utils/tooltip.service';
import { overriddenPublicUrlKey } from './views/app/app.component';

function getPublicBasePath() {
  return localStorage.getItem(overriddenPublicUrlKey) || environment.publicApiUrl;
}

// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService): Configuration {
  return new Configuration({
    basePath: getPublicBasePath(),
    accessToken: () => signInService.currentAccessToken
  });
}

export function getConfigService(http: Http) {
  return new ConfigService(http, getPublicBasePath(), null);
}

@NgModule({
  imports: [
    ApiModule,
    DataBrowserModule,
    SharedModule,
  ],
  declarations: [
    AppComponent,
  ],
  providers: [
    {
      provide: ConfigService,
      useFactory: getConfigService,
      deps: [Http]
    },
    {
      provide: Configuration,
      deps: [SignInService],
      useFactory: getConfiguration
    },
    DbConfigService,
    TooltipService,
    ServerConfigService,
    {
      provide: ErrorHandler,
      deps: [ServerConfigService],
      useClass: ErrorReporterService,
    },
    SignInService,
  ],
  // This specifies the top-level components, to load first.
  bootstrap: [AppComponent]

})
export class AppModule {}
