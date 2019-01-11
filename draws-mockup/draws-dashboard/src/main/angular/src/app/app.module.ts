import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { PageHeaderComponent } from './page-header/page-header.component';
import { OusWidgetComponent } from './ous-widget/ous-widget.component';
import {AppService} from "./app.service";
import {HttpClientModule } from "@angular/common/http";
import {SuiModule} from "ng2-semantic-ui";
import {AppConfig} from "./app.config";

@NgModule({
  declarations: [
    AppComponent,
    PageHeaderComponent,
    OusWidgetComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    SuiModule
  ],
  providers: [AppConfig, AppService],
  bootstrap: [AppComponent]
})
export class AppModule { }
