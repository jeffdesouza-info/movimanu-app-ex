import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ManualMovementsPageComponent } from './manual-movements-page.component';
import {
  CosifResponse,
  IncludeMovementRequest,
  ManualMovementPageResponse,
  ManualMovementResponse,
  ProductResponse,
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

  it('starts with disabled fields and enables the form after Novo loads products', () => {
    const fixture = createLoadedFixture();
    const host = fixture.nativeElement as HTMLElement;

    expect(
      Array.from(host.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
        '#mes, #ano, #codigoProduto, #codigoCosif, #valor, #descricao',
      )).every((field) => field.disabled),
    ).toBe(true);
    expect(buttonByText(host, 'Novo')?.disabled).toBe(false);
    expect(buttonByText(host, 'Limpar')?.disabled).toBe(true);
    expect(buttonByText(host, 'Incluir')?.disabled).toBe(true);

    buttonByText(host, 'Novo')?.click();
    fixture.detectChanges();
    expect(host.textContent).toContain('Carregando produtos...');
    expectProductsRequest().flush(products());
    fixture.detectChanges();

    expect(fixture.componentInstance.formMode()).toBe('including');
    expect(fixture.componentInstance.movementForm.controls.mes.enabled).toBe(true);
    expect(fixture.componentInstance.movementForm.controls.ano.enabled).toBe(true);
    expect(fixture.componentInstance.movementForm.controls.codigoProduto.enabled).toBe(true);
    expect(fixture.componentInstance.movementForm.controls.codigoCosif.disabled).toBe(true);
    expect(fixture.componentInstance.movementForm.controls.valor.enabled).toBe(true);
    expect(fixture.componentInstance.movementForm.controls.descricao.enabled).toBe(true);
    expect(host.querySelector('#codigoProduto')?.textContent).toContain('-- Selecione --');
    expect(host.querySelector('#codigoProduto')?.textContent).toContain('Conta corrente');
    expect(host.querySelector('#codigoCosif')?.textContent).toContain('-- Selecione --');
  });

  it('shows COSIF loading and error states, then retries only the current product GET', () => {
    const fixture = createLoadedFixture();
    startInclusion(fixture);
    const component = fixture.componentInstance;
    component.movementForm.patchValue({ mes: '12', ano: '2026', valor: '150,5', descricao: '  Ajuste  ' });
    component.movementForm.controls.codigoProduto.setValue('0001');

    const firstCosifRequest = expectCosifRequest('0001');
    fixture.detectChanges();
    expect(component.cosifsState()).toBe('loading');
    expect(component.movementForm.controls.codigoCosif.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Carregando COSIFs...');

    firstCosifRequest.flush(
      { detail: 'Falha temporária' },
      { status: 503, statusText: 'Service Unavailable' },
    );
    fixture.detectChanges();
    expect(component.cosifsState()).toBe('error');
    expect(component.cosifs()).toEqual([]);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();
    expect(component.movementForm.controls.codigoCosif.disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os COSIFs.');
    expect(component.movementForm.getRawValue()).toMatchObject({
      mes: '12',
      ano: '2026',
      codigoProduto: '0001',
      valor: '150,5',
      descricao: '  Ajuste  ',
    });
    expect(component.includeEnabled()).toBe(false);

    buttonByText(fixture.nativeElement as HTMLElement, 'Tentar novamente')?.click();
    fixture.detectChanges();
    const retry = expectCosifRequest('0001');
    httpTesting.expectNone((request) => request.method === 'POST');
    retry.flush(cosifs());
    fixture.detectChanges();

    expect(component.cosifsState()).toBe('loaded');
    expect(component.movementForm.controls.codigoCosif.enabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();
    expect(component.cosifs()).toEqual(cosifs());
    expect(component.movementForm.controls.valor.value).toBe('150,5');
    expect(component.movementForm.controls.descricao.value).toBe('  Ajuste  ');
    expect(component.includeEnabled()).toBe(false);
  });

  it('discards COSIF responses from an earlier product selection', () => {
    const fixture = createLoadedFixture();
    startInclusion(fixture);
    const component = fixture.componentInstance;

    component.movementForm.controls.codigoProduto.setValue('0001');
    const firstRequest = expectCosifRequest('0001');
    component.movementForm.controls.codigoProduto.setValue('0002');
    expect(firstRequest.cancelled).toBe(true);
    const currentRequest = expectCosifRequest('0002');
    currentRequest.flush([{ codigoCosif: '20000000002', codigoClassificacao: '654321' }]);
    fixture.detectChanges();

    expect(component.cosifs()).toEqual([
      { codigoCosif: '20000000002', codigoClassificacao: '654321' },
    ]);
    expect(component.cosifsState()).toBe('loaded');
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();
  });

  it('clears fields and cancels a pending COSIF lookup without leaving inclusion mode', () => {
    const fixture = createLoadedFixture();
    startInclusion(fixture);
    const component = fixture.componentInstance;
    component.movementForm.patchValue({ mes: '3', ano: '2025', valor: '-150,00', descricao: 'Ajuste' });
    component.movementForm.controls.codigoProduto.setValue('0001');
    const pendingCosifRequest = expectCosifRequest('0001');

    buttonByText(fixture.nativeElement as HTMLElement, 'Limpar')?.click();
    fixture.detectChanges();

    expect(pendingCosifRequest.cancelled).toBe(true);
    expect(component.formMode()).toBe('including');
    expect(component.movementForm.getRawValue()).toEqual({
      mes: '',
      ano: '',
      codigoProduto: null,
      codigoCosif: null,
      valor: '',
      descricao: '',
    });
    expect(component.movementForm.controls.mes.enabled).toBe(true);
    expect(component.movementForm.controls.codigoProduto.enabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.disabled).toBe(true);
    expect(component.cosifs()).toEqual([]);
    expect(component.cosifsState()).toBe('idle');
    expect(component.includeEnabled()).toBe(false);
  });

  it('requests the same product again after clearing a completed COSIF lookup', () => {
    const fixture = createLoadedFixture();
    startInclusion(fixture);
    const component = fixture.componentInstance;
    component.movementForm.controls.codigoProduto.setValue('0001');
    expectCosifRequest('0001').flush(cosifs());
    fixture.detectChanges();

    expect(component.cosifsState()).toBe('loaded');
    expect(component.movementForm.controls.codigoCosif.enabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();

    buttonByText(fixture.nativeElement as HTMLElement, 'Limpar')?.click();
    fixture.detectChanges();
    component.movementForm.controls.codigoProduto.setValue('0001');

    const repeatedRequest = expectCosifRequest('0001');
    fixture.detectChanges();
    expect(component.cosifsState()).toBe('loading');
    expect(component.cosifs()).toEqual([]);
    expect(component.movementForm.controls.codigoCosif.disabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();

    repeatedRequest.flush(cosifs());
    fixture.detectChanges();
    expect(component.cosifsState()).toBe('loaded');
    expect(component.movementForm.controls.codigoCosif.enabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();
  });

  it('cancels and repeats the same product lookup after clearing while it is pending', () => {
    const fixture = createLoadedFixture();
    startInclusion(fixture);
    const component = fixture.componentInstance;
    component.movementForm.controls.codigoProduto.setValue('0001');
    const pendingRequest = expectCosifRequest('0001');
    expect(component.cosifsState()).toBe('loading');
    expect(component.movementForm.controls.codigoCosif.disabled).toBe(true);

    buttonByText(fixture.nativeElement as HTMLElement, 'Limpar')?.click();
    fixture.detectChanges();
    expect(pendingRequest.cancelled).toBe(true);
    component.movementForm.controls.codigoProduto.setValue('0001');

    const repeatedRequest = expectCosifRequest('0001');
    fixture.detectChanges();
    expect(component.cosifsState()).toBe('loading');
    expect(component.cosifs()).toEqual([]);
    expect(component.movementForm.controls.codigoCosif.disabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();

    repeatedRequest.flush(cosifs());
    fixture.detectChanges();
    expect(component.cosifsState()).toBe('loaded');
    expect(component.movementForm.controls.codigoCosif.enabled).toBe(true);
    expect(component.movementForm.controls.codigoCosif.value).toBeNull();
  });

  it('validates fields, preserves invalid text, formats valid money, and emits the exact API draft', () => {
    const fixture = createLoadedFixture();
    startInclusion(fixture);
    const component = fixture.componentInstance;
    component.movementForm.controls.codigoProduto.setValue('0001');
    expectCosifRequest('0001').flush(cosifs());
    fixture.detectChanges();

    component.movementForm.patchValue({
      mes: '12',
      ano: '2026',
      codigoCosif: '10000000001',
      valor: '-150,00',
      descricao: '  Ajuste manual  ',
    });
    component.formatValueOnBlur();
    expect(component.movementForm.controls.valor.value).toBe('-150,00');
    expect(component.includeEnabled()).toBe(true);
    fixture.detectChanges();

    let emitted: IncludeMovementRequest | undefined;
    component.includeRequested.subscribe((request) => (emitted = request));
    buttonByText(fixture.nativeElement as HTMLElement, 'Incluir')?.click();
    fixture.detectChanges();

    expect(emitted).toEqual({
      mes: 12,
      ano: 2026,
      codigoProduto: '0001',
      codigoCosif: '10000000001',
      descricao: '  Ajuste manual  ',
      valor: '-150.00',
    });
    httpTesting.expectNone((request) => request.method === 'POST');

    component.movementForm.controls.valor.setValue('1,234');
    component.movementForm.controls.valor.markAsTouched();
    component.formatValueOnBlur();
    fixture.detectChanges();
    expect(component.movementForm.controls.valor.value).toBe('1,234');
    expect(component.fieldError('valor')).toContain('valor decimal');
    expect(fixture.nativeElement.textContent).toContain('valor decimal');
    expect(component.includeEnabled()).toBe(false);

    component.movementForm.controls.descricao.setValue('   ');
    component.movementForm.controls.descricao.markAsTouched();
    fixture.detectChanges();
    expect(component.fieldError('descricao')).toContain('caractere diferente de espaço');
    expect(fixture.nativeElement.textContent).toContain(
      'caractere diferente de espaço',
    );
    expect(component.movementForm.controls.descricao.value).toBe('   ');
    expect(component.includeEnabled()).toBe(false);

    component.movementForm.controls.descricao.setValue(`${'x'.repeat(50)} `);
    component.movementForm.controls.valor.setValue('9999999999999999,99');
    fixture.detectChanges();
    expect(component.movementForm.controls.descricao.hasError('maxlength')).toBe(true);
    expect(component.includeEnabled()).toBe(false);
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

function expectProductsRequest() {
  return TestBed.inject(HttpTestingController).expectOne(
    (request) => request.method === 'GET' && request.url === '/api/v1/produtos',
  );
}

function expectCosifRequest(codigoProduto: string) {
  return TestBed.inject(HttpTestingController).expectOne(
    (request) =>
      request.method === 'GET' &&
      request.url === `/api/v1/produtos/${codigoProduto}/cosifs`,
  );
}

function createLoadedFixture(): ComponentFixture<ManualMovementsPageComponent> {
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
  return fixture;
}

function startInclusion(fixture: ComponentFixture<ManualMovementsPageComponent>): void {
  buttonByText(fixture.nativeElement as HTMLElement, 'Novo')?.click();
  fixture.detectChanges();
  expectProductsRequest().flush(products());
  fixture.detectChanges();
}

function buttonByText(host: HTMLElement, text: string): HTMLButtonElement | null {
  return (
    Array.from(host.querySelectorAll<HTMLButtonElement>('button')).find(
      (button) => button.textContent?.trim() === text,
    ) ?? null
  );
}

function products(): ProductResponse[] {
  return [
    { codigoProduto: '0001', descricaoProduto: 'Conta corrente' },
    { codigoProduto: '0002', descricaoProduto: 'Poupança' },
  ];
}

function cosifs(): CosifResponse[] {
  return [{ codigoCosif: '10000000001', codigoClassificacao: '123456' }];
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
