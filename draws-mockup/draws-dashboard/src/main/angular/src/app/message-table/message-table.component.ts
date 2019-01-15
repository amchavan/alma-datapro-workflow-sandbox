import { Component, Input, OnInit } from '@angular/core';
import { AppService } from "../app.service";
import { Message } from '../message';

@Component({
  selector: 'message-table',
  templateUrl: './message-table.component.html',
  styleUrls: ['./message-table.component.css']
})
export class MessageTableComponent implements OnInit {

  messages: Message[];

  constructor(public appService : AppService) {
  }

  ngOnInit() {
    this.appService.loadMessages();
    this.messages = this.appService.messages;
  }
}
