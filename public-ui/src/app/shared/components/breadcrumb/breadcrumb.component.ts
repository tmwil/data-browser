import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { environment } from '../../../../environments/environment';
import { DbConfigService } from '../../../utils/db-config.service';

export interface Breadcrumb {
  label: string;
  isIntermediate: boolean;
  url: string;
}
@Component({
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: [
    '../../../styles/buttons.css',
    '../../../styles/page.css',
    '../../../styles/headers.css',
    '../../../styles/inputs.css',
    './breadcrumb.component.css']
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  subscription: Subscription;
  breadcrumbs: Breadcrumb[];
  allOfUs = environment.researchAllOfUsUrl;
  workBenchIsBeta = environment.workBenchIsBeta;
  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private dbc: DbConfigService) { }

  /**
   * Generate a breadcrumb using the default label and url. Uses the route's
   * paramMap to do any necessary variable replacement. For example, if we
   * have a label value of ':wsid' as defined in a route's breadcrumb, we can
   * do substitution with the 'wsid' value in the route's paramMap.
   */
  private static makeBreadcrumb(label: string,
    isIntermediate: boolean,
    url: string,
    route: ActivatedRoute): Breadcrumb {
    let newLabel = label;
    // Perform variable substitution in label only if needed.
    if (newLabel.indexOf(':') >= 0) {
      const paramMap = route.snapshot.paramMap;
      for (const k of paramMap.keys) {
        if (paramMap.get(k).indexOf('?') >= 0) {
          newLabel = newLabel.replace(':' + k, paramMap.get(k).substring(0, paramMap.get(k).indexOf('?')));
        } else {
          newLabel = newLabel.replace(':' + k, paramMap.get(k));
        }
      }
    }
    if (newLabel.toLowerCase().indexOf('utilization') > 0) {
      newLabel = newLabel.replace('and', '&');
    }
    return {
      label: newLabel,
      isIntermediate: isIntermediate,
      url: url
    };
  }

  /**
   * Filters an array of Breadcrumbs so that the last element is never an intermediateBreadcrumb
   * This ensures that breadcrumb headers are displayed correctly while still tracking
   * intermediate pages.
   */
  private static filterBreadcrumbs(breadcrumbs: Breadcrumb[]): Array<Breadcrumb> {
    if (breadcrumbs.length > 0) {
      let last = breadcrumbs[breadcrumbs.length - 1];
      while ((breadcrumbs.length > 1) && (last.isIntermediate)) {
        breadcrumbs.pop();
        last = breadcrumbs[breadcrumbs.length - 1];
      }
    }
    return breadcrumbs;
  }

  ngOnInit() {
    this.breadcrumbs = this.buildBreadcrumbs(this.activatedRoute.root);
    this.subscription = this.router.events.filter(event => event instanceof NavigationEnd)
      .subscribe(event => {
        this.breadcrumbs = BreadcrumbComponent.filterBreadcrumbs(
          this.buildBreadcrumbs(this.activatedRoute.root));
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  /**
   * Returns array of Breadcrumb objects that represent the breadcrumb trail.
   * Derived from current route in conjunction with the overall route structure.
   */
  private buildBreadcrumbs(route: ActivatedRoute,
    url: string = '',
    breadcrumbs: Breadcrumb[] = []): Array<Breadcrumb> {
    const children: ActivatedRoute[] = route.children;
    const routeDataBreadcrumb = 'breadcrumb';
    if (children.length === 0) {
      return breadcrumbs;
    }
    for (const child of children) {
      if (!child.snapshot.data.hasOwnProperty(routeDataBreadcrumb)) {
        return this.buildBreadcrumbs(child, url, breadcrumbs);
      }
      const routeURL: string = child.snapshot.url.map(segment => segment.path).join('/');
      if (routeURL.length > 0) {
        url += `/${routeURL}`;
      }
      const label = child.snapshot.data[routeDataBreadcrumb].value;
      const isIntermediate = child.snapshot.data[routeDataBreadcrumb].intermediate;

      if (!breadcrumbs.some(b => b.url === url)) {
        const breadcrumb = BreadcrumbComponent.makeBreadcrumb(label, isIntermediate, url, child);
        breadcrumbs.push(breadcrumb);
      }
      return this.buildBreadcrumbs(child, url, breadcrumbs);
    }
  }

}
