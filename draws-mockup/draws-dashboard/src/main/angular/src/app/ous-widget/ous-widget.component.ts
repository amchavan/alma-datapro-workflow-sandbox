import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'ous-widget',
  templateUrl: './ous-widget.component.html',
  styleUrls: ['./ous-widget.component.css']
})
export class OusWidgetComponent implements OnInit {

  @Input('entityId') entityId: string;
  @Input('timestamp') timestamp: string;

  constructor() { }

  ngOnInit() {
  }

}
