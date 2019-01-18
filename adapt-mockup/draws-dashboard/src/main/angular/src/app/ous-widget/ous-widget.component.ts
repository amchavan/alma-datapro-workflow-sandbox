import { Component, Input, OnInit } from '@angular/core';
import { Ous } from '../ous';

@Component({
  selector: 'ous-widget',
  templateUrl: './ous-widget.component.html',
  styleUrls: ['./ous-widget.component.css']
})
export class OusWidgetComponent implements OnInit {

  @Input('ous') ous: Ous;
  // @Input('entityId') entityId: string;
  // @Input('timestamp') timestamp: string;
  // @Input('substate') substate: string;

  constructor() { }

  ngOnInit() {
  }

}
