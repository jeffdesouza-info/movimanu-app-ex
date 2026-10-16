import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ManualMovementsPageComponent } from './manual-movements-page.component';
import {
  ManualMovementPageResponse,
  ManualMovementResponse,
} from './manual-movements.models';

describe('ManualMovementsPageComponent', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManualMovementsPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the first page and renders the seven columns in order', () => {
    const fixture = TestBed.createComponent(ManualMovementsPageComponent);
    fixture.detectChanges();

    const initialRequest = expectPageRequest(0);
    expect(initialRequest.request.method).toBe('GET');
    expect(fixture.nativeElement.textContent).toContain('Carregando movimentos...');

    const response = pageResponse(0, 7, 2, 6);
    response.content[0] = { ...movement(1), mes: 1, ano: 2026 };
    response.content[1] = { ...movement(2), mes: 12, ano: 2025 };
    initialRequest.flush(response);
    fixture.detectChanges();

    const headers = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('thead th'),
    ).map((header) => header.textContent?.trim());
    expect(headers).toEqual([
      'Mês',
      'Ano',
      'Código do Produto',
      'Descrição do Produto',
      'Nr Lançamento',
      'Descrição do Movimento',
      'Valor',
    ]);

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr.movement-row');
    expect(rows.length).toBe(6);
    expect(rows[0].querySelectorAll('td')[1].textContent?.trim()).toBe('2026');
    expect(rows[1].querySelectorAll('td')[0].textContent?.trim()).toBe('12');
    expect(rows[1].querySelectorAll('td')[1].textContent?.trim()).toBe('2025');
    expect(rows[0].textContent).toContain('150,50');
    expect(fixture.nativeElement.textContent).toContain('Exibindo 1-6 de 7 movimentos');
  });

  it('requests the next and previous pages using the API metadata', () => {
    const fixture = TestBed.createComponent(ManualMovementsPageComponent);
    fixture.detectChanges();
    expectPageRequest(0).flush(pageResponse(0, 7, 2, 6));
    fixture.detectChanges();

    const nextButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Próxima página"]',
    );
    nextButton?.click();
    fixture.detectChanges();

    expectPageRequest(1).flush(pageResponse(1, 7, 2, 1));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Página 2 de 2');
    expect(
      (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr.movement-row').length,
    ).toBe(1);

    const previousButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Página anterior"]',
    );
    previousButton?.click();
    fixture.detectChanges();
    expectPageRequest(0).flush(pageResponse(0, 7, 2, 6));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Página 1 de 2');
  });

  it('shows a safe summary and return navigation for an empty page beyond the results', () => {
    const fixture = TestBed.createComponent(ManualMovementsPageComponent);
    fixture.detectChanges();
    expectPageRequest(0).flush(pageResponse(0, 7, 2, 6));
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('[aria-label="Próxima página"]')
      ?.click();
    fixture.detectChanges();

    const outOfRangePage: ManualMovementPageResponse = {
      content: [],
      page: 2,
      size: 6,
      totalElements: 7,
      totalPages: 2,
    };
    expectPageRequest(1).flush(outOfRangePage);
    fixture.detectChanges();

    const pageText = fixture.nativeElement.textContent;
    expect(pageText).toContain('Nenhum movimento nesta página. Total: 7 movimentos.');
    expect(pageText).not.toContain('Exibindo');
    expect(pageText).toContain('Página 2 de 2');
    expect(fixture.componentInstance.pageData()).toEqual(outOfRangePage);

    const firstPageButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Primeira página"]',
    );
    const previousButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Página anterior"]',
    );
    const nextButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Próxima página"]',
    );
    expect(firstPageButton?.disabled).toBe(false);
    expect(previousButton?.disabled).toBe(false);
    expect(nextButton?.disabled).toBe(true);

    previousButton?.click();
    fixture.detectChanges();
    expectPageRequest(1).flush(pageResponse(1, 7, 2, 1));
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('[aria-label="Primeira página"]')
      ?.click();
    fixture.detectChanges();
    expectPageRequest(0).flush(pageResponse(0, 7, 2, 6));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Página 1 de 2');
  });

  it('keeps first and previous navigation available when totalPages is one', () => {
    const fixture = TestBed.createComponent(ManualMovementsPageComponent);
    fixture.detectChanges();

    expectPageRequest(0).flush({
      content: [],
      page: 2,
      size: 6,
      totalElements: 3,
      totalPages: 1,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Nenhum movimento nesta página. Total: 3 movimentos.',
    );
    expect(fixture.nativeElement.textContent).toContain('Página 1 de 1');

    const firstPageButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Primeira página"]',
    );
    const previousButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Página anterior"]',
    );
    expect(firstPageButton?.disabled).toBe(false);
    expect(previousButton?.disabled).toBe(false);

    previousButton?.click();
    fixture.detectChanges();
    expectPageRequest(1).flush({
      content: [],
      page: 1,
      size: 6,
      totalElements: 3,
      totalPages: 1,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Página 1 de 1');
  });

  it('shows the empty state when the API returns no movements', () => {
    const fixture = TestBed.createComponent(ManualMovementsPageComponent);
    fixture.detectChanges();

    expectPageRequest(0).flush({
      content: [],
      page: 0,
      size: 6,
      totalElements: 0,
      totalPages: 0,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum movimento encontrado.');
    expect(fixture.nativeElement.textContent).toContain('0 movimentos encontrados');
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[aria-label="Paginação de movimentos"]'),
    ).toBeNull();
  });

  it('shows the API error and retries only the requested GET page', () => {
    const fixture = TestBed.createComponent(ManualMovementsPageComponent);
    fixture.detectChanges();

    expectPageRequest(0).flush(
      { detail: 'Falha temporária de consulta' },
      { status: 503, statusText: 'Service Unavailable' },
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Falha temporária de consulta');

    const retryButton = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.retry-button',
    );
    retryButton?.click();
    fixture.detectChanges();

    expectPageRequest(0).flush({
      content: [],
      page: 0,
      size: 6,
      totalElements: 0,
      totalPages: 0,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhum movimento encontrado.');
  });
});

function expectPageRequest(page: number) {
  return TestBed.inject(HttpTestingController).expectOne(
    (request) =>
      request.method === 'GET' &&
      request.url === '/api/v1/movimentos-manuais' &&
      request.params.get('page') === String(page),
  );
}

function pageResponse(
  page: number,
  totalElements: number,
  totalPages: number,
  numberOfMovements: number,
): ManualMovementPageResponse {
  const content = Array.from({ length: numberOfMovements }, (_, index) =>
    movement(index + 1 + page * 6),
  );

  return { content, page, size: 6, totalElements, totalPages };
}

function movement(number: number): ManualMovementResponse {
  return {
    mes: 1,
    ano: 2026,
    codigoProduto: String(number).padStart(4, '0'),
    descricaoProduto: `Produto ${number}`,
    numeroLancamento: String(number),
    descricao: `Movimento ${number}`,
    valor: '150.50',
  };
}
