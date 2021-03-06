import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable()
export class HeaderFooterService {
  menu: any;
  workBenchIsBeta: boolean;
  allOfUsUrl: any;

  constructor() {
    this.workBenchIsBeta = environment.workBenchIsBeta;
    this.allOfUsUrl = environment.researchAllOfUsUrl;
    this.menu = [
      {
        title: 'about',
        url: 'https://www.researchallofus.org/about-the-research-hub/',
        submenu: true,
        sub0: [{
          title: 'About the Research Hub ',
          url: 'https://www.researchallofus.org/about-the-research-hub/',
          submenu: false
        },
          {
            title: 'Researchers as Partners',
            url: 'https://www.researchallofus.org/researchers-as-partners/',
            submenu: true,
            sub1: [{
              title: 'Researcher Workshops and Public Input',
              url: 'https://www.researchallofus.org/researchers-as-partners/researcher-workshops-and-public-input/',
              submenu: false
            }]
          },
          {
            title: 'Privacy & Security Protocols',
            url: 'https://www.researchallofus.org/privacy-security-protocols/',
            submenu: false
          },
          {
            title: 'Research Hub Updates',
            url: 'https://www.researchallofus.org/research-hub-updates/',
            submenu: false
          }],
      },
      {
        title: 'data',
        url: 'https://www.researchallofus.org/data-snapshots/',
        submenu: true,
        sub0: [{
          title: 'Data Snapshots',
          url: 'https://www.researchallofus.org/data-snapshots/',
          submenu: false
        },
          {
            title: 'Data Browser',
            url: 'https://databrowser.researchallofus.org/',
            submenu: false
          },
          {
            title: 'Data Sources',
            url: 'https://www.researchallofus.org/data-sources/',
            submenu: true,
            sub1: [{
              title: 'Methods',
              url: 'https://www.researchallofus.org/data-sources/methods/',
              submenu: false
            },
              {
                title: 'Survey Explorer',
                url: 'https://www.researchallofus.org/data-sources/survey-explorer/',
                submenu: false
              }]
          }]
      },
      {
        title: 'tools',
        url: 'https://www.researchallofus.org/workbench/',
        submenu: true,
        sub0: [{
          title: 'Workbench',
          url: 'https://www.researchallofus.org/workbench/',
          submenu: false,
        }]
      }
    ];
    if (this.workBenchIsBeta) {
      this.menu[0].sub0[1].submenu = false;
      delete this.menu[0].sub0[1]['sub1'];
      this.menu[0].sub0.splice(2, 0, {
        title: 'Researcher Workshops',
        url: 'https://www.researchallofus.org/researchers-as-partners/researcher-workshops-and-public-input/',
        submenu: false
      });
      this.menu[1].sub0 = [
        {
          title: 'Data Snapshots',
          url: 'https://www.researchallofus.org/data-snapshots/',
          submenu: false
        },
        {
          title: 'Data Sources',
          url: 'https://www.researchallofus.org/data-sources/',
          submenu: false
        },
        {
          title: 'Data Methods',
          url: 'https://www.researchallofus.org/data-sources/methods/',
          submenu: false
        },
        {
          title: 'Data Available',
          url: 'https://www.researchallofus.org/data-available/',
          submenu: false
        },
        {
          title: 'Data Use Policies',
          url: 'https://www.researchallofus.org/data-use-policies',
          submenu: false
        }
      ];
      this.menu[0].sub0[3].title = 'Privacy & Security';
      this.menu[2].sub0 = [
        {
          title: 'Survey Explorer',
          url: 'https://www.researchallofus.org/data-sources/survey-explorer/',
          submenu: false
        },
        {
          title: 'Data Browser',
          url: 'https://databrowser.researchallofus.org/',
          submenu: false
        },
        {
          title: 'Researcher Workbench',
          url: 'https://www.researchallofus.org/workbench/',
          submenu: false,
        },
        {
          title: 'Guides & Support',
          url: 'https://www.researchallofus.org/guides-support/',
          submenu: false,
        }
      ];
      this.menu[3] = {
        title: 'Discover',
        url: '',
        submenu: true,
        sub0: [
          {
            title: 'Researcher Projects Directory',
            url: 'https://www.researchallofus.org/research-projects-directory/',
            submenu: false
          },
          {
            title: 'Publications',
            url: 'https://www.researchallofus.org/publications',
            submenu: false
          }
        ],
        sub1: [
          {
            title: 'FAQ',
            url: this.allOfUsUrl + '/frequently-asked-questions/',
            submenu: false
          }
        ],
        sub2: [
          {
            title: `WE'RE IN BETA`,
            url: '#',
            submenu: false
          }
        ]
      };
    }
  }
}
