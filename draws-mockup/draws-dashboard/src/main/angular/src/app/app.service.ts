import {Ous} from "./ous";
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Injectable} from "@angular/core";
import {AppConfig} from "./app.config";
import { throwError } from "rxjs";
import { Message } from "./message";

@Injectable({providedIn: 'root'})
export class AppService {

  fullyObservedOUSs : Ous[];
  readyForProcessingOUSs : Ous[];
  processingOUSs : Ous[];
  processingProblemOUSs : Ous[];
  readyForReviewOUSs : Ous[];
  reviewingOUSs : Ous[];
  verifiedOUSs : Ous[];
  deliveryInProgressOUSs : Ous[];
  qa3InProgressOUSs : Ous[];
  deliveredOUSs : Ous[];

  messages : Message[];

  constructor( public http: HttpClient, public config : AppConfig ){

  }

  loadOUSs() {

      this.requestOusByState("FullyObserved");
      this.requestOusByState("ReadyForProcessing");
      this.requestOusByState("Processing");
      this.requestOusByState("ProcessingProblem");
      this.requestOusByState("ReadyForReview");
      this.requestOusByState("Reviewing");
      this.requestOusByState("Verified");
      this.requestOusByState("DeliveryInProgress");
      this.requestOusByState("QA3InProgress");
      this.requestOusByState("Delivered");
  }

  setObsUnitSetsByState( state : string, obsUnitSets : Ous[] ) {
    if ( state == "FullyObserved") {
      this.fullyObservedOUSs = obsUnitSets;
    } else if ( state == "ReadyForProcessing") {
      this.readyForProcessingOUSs = obsUnitSets;
    } else if ( state == "Processing") {
      this.processingOUSs = obsUnitSets;
    } else if ( state == "ProcessingProblem") {
      this.processingProblemOUSs = obsUnitSets;
    } else if ( state == "ReadyForReview") {
      this.readyForReviewOUSs = obsUnitSets;
    } else if ( state == "Reviewing") {
      this.reviewingOUSs = obsUnitSets;
    } else if ( state == "Verified") {
      this.verifiedOUSs = obsUnitSets;
    } else if ( state == "DeliveryInProgress") {
      this.deliveryInProgressOUSs = obsUnitSets;
    } else if ( state == "QA3InProgress") {
      this.qa3InProgressOUSs = obsUnitSets;
    } else if ( state == "Delivered") {
      this.deliveredOUSs = obsUnitSets;
    }
    else {
      throwError;
    }
  }

  requestOusByState(state : string ) : Ous[] {
    let result = [];

    let requestURL = this.config.serverPath+"/"+this.config.ousAPI+"?"+this.config.stateAPIParameter+"="+state;
    let headers = new HttpHeaders();
    headers.append("Authorization", "Basic " + btoa(this.config.username+":"+this.config.password));
    headers.append("Content-Type", "application/x-www-form-urlencoded");

    this.http.get(requestURL, {headers:headers}).subscribe(
      data => {
        result = data as Ous [];
        this.setObsUnitSetsByState( state, result );
        console.log( state+" "+JSON.stringify( result ))
      },
      (err: HttpErrorResponse) => {
        console.log (err.message);
      }
    )
    return null;
  }

  loadMessages() {

    var msg = new Message;
    msg.body = '{"name":"abcd","value":"1"}';
    msg.queue = "test.queue";
    msg.state = "Sent";
    msg.timestamp = "2018-12-10T16:16:07"

    this.messages = [msg];
  }
}
