import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { OusWidgetComponent } from './ous-widget.component';

describe('OusWidgetComponent', () => {
  let component: OusWidgetComponent;
  let fixture: ComponentFixture<OusWidgetComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OusWidgetComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OusWidgetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
