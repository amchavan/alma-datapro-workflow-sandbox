import { Component, Input, OnInit } from '@angular/core';
import { Ous } from '../ous';
import { AppService } from "../app.service";

@Component({
  selector: 'ous-table',
  templateUrl: './ous-table.component.html',
  styleUrls: ['./ous-table.component.css']
})
export class OusTableComponent implements OnInit {

  constructor(public appService : AppService) {
  }

  ngOnInit() {
    this.appService.loadOUSs();
  }
}
