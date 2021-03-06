import { Component } from '@angular/core';

@Component({
  selector: 'app-intro-vids',
  templateUrl: './intro-vids.component.html',
  styleUrls: ['../../../styles/template.css', './intro-vids.component.css']
})
export class IntroVidsComponent {

  videos = [{
    title: 'Participant Intro',
    downloadUrl: '',
    poster: '/assets/videos/participant_video_poster.png',
    src: [{
      url: '/assets/videos/participant_intro.mp4',
      type: 'video/mp4'
    },
    {
      url: '/assets/videos/participant_intro.ogv',
      type: 'video/ogg'
    },
    {
      url: '/assets/videos/participant_intro.webm',
      type: 'video/webm'
    }],
  },
  {
    title: 'Researcher Intro',
    downloadUrl: '',
    poster: '/assets/videos/researcher_video_poster.png',
    src: [{
      url: '/assets/videos/researcher_intro.mp4',
      type: 'video/mp4'
    },
    {
      url: '/assets/videos/researcher_intro.ogv',
      type: 'video/ogg'
    },
    {
      url: '/assets/videos/researcher_intro.webm',
      type: 'video/webm'
    }],
  },
  {
    title: 'Surveys Intro',
    downloadUrl: '',
    poster: '/assets/videos/surveys_video_poster.png',
    src: [{
      url: '/assets/videos/surveys_intro.mp4',
      type: 'video/mp4'
    },
    {
      url: '/assets/videos/surveys_intro.ogv',
      type: 'video/ogg'
    },
    {
      url: '/assets/videos/surveys_intro.webm',
      type: 'video/webm'
    }],
  },
  {
    title: 'Physical Measurements Intro',
    downloadUrl: '',
    poster: '/assets/videos/physical_measurements_video_poster.png',
    src: [{
      url: '/assets/videos/physical_measurements_intro.mp4',
      type: 'video/mp4'
    },
    {
      url: '/assets/videos/physical_measurements_intro.ogv',
      type: 'video/ogg'
    },
    {
      url: '/assets/videos/physical_measurements_intro.webm',
      type: 'video/webm'
    }],
  }];

}
