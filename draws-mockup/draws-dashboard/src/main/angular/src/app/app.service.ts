import {Ous} from "./ous";
import {HttpClient, HttpErrorResponse, HttpHeaders} from '@angular/common/http';
import {Injectable} from "@angular/core";
import {AppConfig} from "./app.config";

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

  constructor(public http: HttpClient, public config : AppConfig){
  }


  loadOUSs() {

      this.fullyObservedOUSs = this.requestOusByState("FullyObserved");
      this.readyForProcessingOUSs = this.requestOusByState("ReadyForProcessing");
      this.processingOUSs = this.requestOusByState("Processing");
      this.processingProblemOUSs = this.requestOusByState("ProcessingProblem");
      this.readyForReviewOUSs = this.requestOusByState("ReadyForReview");
      this.reviewingOUSs = this.requestOusByState("Reviewing");
      this.verifiedOUSs = this.requestOusByState("Verified");
      this.deliveryInProgressOUSs = this.requestOusByState("DeliveryInProgress");
      this.qa3InProgressOUSs = this.requestOusByState("QA3InProgress");
      this.deliveredOUSs = this.requestOusByState("Delivered");

  }

  readObsUnitSets(state : string, obsUnitSets : Ous[]) {
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
      this.reviewingOUSs == obsUnitSets;
    } else if ( state == "Verified") {
      this.verifiedOUSs = obsUnitSets;
    } else if ( state == "DeliveryInProgress") {
      this.deliveryInProgressOUSs = obsUnitSets;
    } else if ( state == "QA3InProgress") {
      this.qa3InProgressOUSs = obsUnitSets;
    } else if ( state == "Delivered") {
      this.deliveredOUSs = obsUnitSets;
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
        this.readObsUnitSets(state, result);
        console.log(state+" "+JSON.stringify(result))
      },
      (err: HttpErrorResponse) => {
        console.log (err.message);
      }
    );

    return result;
  }


}
