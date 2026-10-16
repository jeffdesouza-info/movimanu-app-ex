import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ManualMovementsApiService } from './features/manual-movements/manual-movements-api.service';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: ManualMovementsApiService,
          useValue: {
            listPage: () =>
              of({ content: [], page: 0, size: 6, totalElements: 0, totalPages: 0 }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the manual movements page', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const heading = (fixture.nativeElement as HTMLElement).querySelector('h1');
    expect(heading?.textContent).toContain('Movimentos Manuais');
  });
});
