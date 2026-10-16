import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, distinctUntilChanged, map, of, startWith, Subject, switchMap, tap } from 'rxjs';
import { ManualMovementsApiService } from './manual-movements-api.service';
import {
  CosifResponse,
  IncludeMovementRequest,
  ManualMovementPageResponse,
  ManualMovementResponse,
  ProductResponse,
} from './manual-movements.models';
import {
  formatMonetaryTextForDisplay,
  integerRangeValidator,
  monetaryTextValidator,
  nonWhitespaceValidator,
  toApiMonetaryText,
} from './manual-movement-form.validators';

type QueryState = 'loading' | 'loaded' | 'error';
type FormMode = 'view' | 'including' | 'sending';
type LookupState = 'idle' | 'loading' | 'loaded' | 'error';
type FormField = 'mes' | 'ano' | 'codigoProduto' | 'codigoCosif' | 'valor' | 'descricao';

type QueryResult =
  | { kind: 'loaded'; page: ManualMovementPageResponse }
  | { kind: 'error'; error: unknown };

type ProductQueryResult =
  | { kind: 'loaded'; products: ProductResponse[] }
  | { kind: 'error' };

type CosifQueryResult =
  | { kind: 'idle' }
  | { kind: 'loaded'; cosifs: CosifResponse[] }
  | { kind: 'error' };

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-manual-movements-page',
  styleUrl: './manual-movements-page.component.css',
  templateUrl: './manual-movements-page.component.html',
})
export class ManualMovementsPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly movementsApi = inject(ManualMovementsApiService);
  private readonly pageRequests = new Subject<number>();
  private readonly productRequests = new Subject<void>();
  private readonly cosifRequests = new Subject<string | null>();

  readonly queryState = signal<QueryState>('loading');
  readonly pageData = signal<ManualMovementPageResponse | null>(null);
  readonly requestedPage = signal(0);
  readonly errorMessage = signal<string | null>(null);
  readonly formMode = signal<FormMode>('view');
  readonly productsState = signal<LookupState>('idle');
  readonly products = signal<ProductResponse[]>([]);
  readonly productErrorMessage = signal<string | null>(null);
  readonly cosifsState = signal<LookupState>('idle');
  readonly cosifs = signal<CosifResponse[]>([]);
  readonly cosifErrorMessage = signal<string | null>(null);
  readonly includeEnabled = signal(false);
  readonly includeErrorMessage = signal<string | null>(null);
  readonly submissionFieldErrors = signal<Partial<Record<FormField, string>>>({});

  readonly movementForm = new FormGroup({
    mes: new FormControl(
      { value: '', disabled: true },
      { nonNullable: true, validators: [Validators.required, integerRangeValidator(1, 12, 2)] },
    ),
    ano: new FormControl(
      { value: '', disabled: true },
      { nonNullable: true, validators: [Validators.required, integerRangeValidator(1, 9999, 4)] },
    ),
    codigoProduto: new FormControl<string | null>({ value: null, disabled: true }, [
      Validators.required,
    ]),
    codigoCosif: new FormControl<string | null>({ value: null, disabled: true }, [
      Validators.required,
    ]),
    valor: new FormControl(
      { value: '', disabled: true },
      { nonNullable: true, validators: [Validators.required, monetaryTextValidator] },
    ),
    descricao: new FormControl(
      { value: '', disabled: true },
      {
        nonNullable: true,
        validators: [Validators.required, nonWhitespaceValidator, Validators.maxLength(50)],
      },
    ),
  });

  readonly firstItem = computed(() => {
    const page = this.pageData();
    if (!page || page.content.length === 0 || page.totalElements === 0) {
      return 0;
    }

    return Math.min(Math.max(page.page, 0) * page.size + 1, page.totalElements);
  });

  readonly lastItem = computed(() => {
    const page = this.pageData();
    if (!page || page.content.length === 0 || this.firstItem() === 0) {
      return 0;
    }

    return Math.min(this.firstItem() + page.content.length - 1, page.totalElements);
  });

  readonly displayedPageNumber = computed(() => {
    const page = this.pageData();
    if (!page || page.totalPages <= 0) {
      return 0;
    }

    return Math.min(Math.max(page.page + 1, 1), page.totalPages);
  });

  readonly showPagination = computed(() => {
    const page = this.pageData();
    return (
      this.queryState() === 'loaded' &&
      !!page &&
      (page.totalPages > 1 || page.page > 0)
    );
  });

  readonly canGoToPreviousPage = computed(
    () => this.queryState() === 'loaded' && (this.pageData()?.page ?? 0) > 0,
  );

  readonly canGoToNextPage = computed(() => {
    const page = this.pageData();
    return this.queryState() === 'loaded' && !!page && page.page + 1 < page.totalPages;
  });

  constructor() {
    this.pageRequests
      .pipe(
        startWith(0),
        tap((page) => {
          this.requestedPage.set(page);
          this.queryState.set('loading');
          this.errorMessage.set(null);
        }),
        switchMap((page) =>
          this.movementsApi.listPage(page).pipe(
            map((response): QueryResult => ({ kind: 'loaded', page: response })),
            catchError((error: unknown) => of<QueryResult>({ kind: 'error', error })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        if (result.kind === 'loaded') {
          this.pageData.set(result.page);
          this.queryState.set('loaded');
          return;
        }

        this.errorMessage.set(this.getErrorMessage(result.error));
        this.queryState.set('error');
      });

    this.productRequests
      .pipe(
        tap(() => {
          this.productsState.set('loading');
          this.productErrorMessage.set(null);
        }),
        switchMap(() =>
          this.movementsApi.listProducts().pipe(
            map((products): ProductQueryResult => ({ kind: 'loaded', products })),
            catchError(() => of<ProductQueryResult>({ kind: 'error' })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        if (result.kind === 'loaded') {
          this.products.set(result.products);
          this.productsState.set('loaded');
        } else {
          this.products.set([]);
          this.productsState.set('error');
          this.productErrorMessage.set(
            'Não foi possível carregar os produtos. Tente novamente.',
          );
        }
      });

    this.cosifRequests
      .pipe(
        switchMap((codigoProduto) => {
          if (codigoProduto === null) {
            return of<CosifQueryResult>({ kind: 'idle' });
          }

          this.cosifsState.set('loading');
          this.cosifErrorMessage.set(null);
          return this.movementsApi.listCosifs(codigoProduto).pipe(
            map((cosifs): CosifQueryResult => ({ kind: 'loaded', cosifs })),
            catchError(() => of<CosifQueryResult>({ kind: 'error' })),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        if (result.kind === 'loaded') {
          this.cosifs.set(result.cosifs);
          this.cosifsState.set('loaded');
          if (this.formMode() === 'including') {
            this.movementForm.controls.codigoCosif.enable({ emitEvent: false });
          }
        } else if (result.kind === 'error') {
          this.cosifs.set([]);
          this.cosifsState.set('error');
          this.cosifErrorMessage.set(
            'Não foi possível carregar os COSIFs. Tente novamente.',
          );
          this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
        } else {
          this.cosifs.set([]);
          this.cosifsState.set('idle');
          this.cosifErrorMessage.set(null);
          this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
        }

        this.updateIncludeEnabled();
      });

    this.movementForm.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.submissionFieldErrors.set({});
      this.includeErrorMessage.set(null);
      this.updateIncludeEnabled();
    });

    this.movementForm.controls.codigoProduto.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((codigoProduto) => this.onProductChanged(codigoProduto));
  }

  startNew(): void {
    if (this.formMode() !== 'view') {
      return;
    }

    this.formMode.set('including');
    this.includeErrorMessage.set(null);
    this.submissionFieldErrors.set({});
    this.movementForm.enable({ emitEvent: false });
    this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
    this.cosifs.set([]);
    this.cosifsState.set('idle');
    this.cosifErrorMessage.set(null);
    this.productRequests.next();
    this.updateIncludeEnabled();
  }

  clearForm(): void {
    if (this.formMode() !== 'including') {
      return;
    }

    this.cosifRequests.next(null);
    this.includeErrorMessage.set(null);
    this.submissionFieldErrors.set({});
    this.movementForm.reset(
      {
        mes: '',
        ano: '',
        codigoProduto: null,
        codigoCosif: null,
        valor: '',
        descricao: '',
      },
    );
    this.movementForm.enable({ emitEvent: false });
    this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
    this.cosifs.set([]);
    this.cosifsState.set('idle');
    this.cosifErrorMessage.set(null);
    this.updateIncludeEnabled();
  }

  retryProducts(): void {
    if (this.formMode() === 'including' && this.productsState() === 'error') {
      this.productRequests.next();
    }
  }

  retryCosifs(): void {
    const codigoProduto = this.movementForm.controls.codigoProduto.value;
    if (
      this.formMode() !== 'including' ||
      !codigoProduto ||
      this.cosifsState() !== 'error'
    ) {
      return;
    }

    this.requestCosifs(codigoProduto);
  }

  includeMovement(): void {
    if (this.formMode() !== 'including') {
      return;
    }

    if (!this.includeEnabled()) {
      this.movementForm.markAllAsTouched();
      return;
    }

    const request = this.createIncludeRequest();
    if (!request) {
      return;
    }

    this.includeErrorMessage.set(null);
    this.submissionFieldErrors.set({});
    this.formMode.set('sending');
    this.includeEnabled.set(false);
    this.movementForm.disable({ emitEvent: false });

    this.movementsApi
      .createMovement(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.resetAfterSuccessfulInclusion();
          this.pageRequests.next(0);
        },
        error: (error: unknown) => this.restoreAfterInclusionFailure(error),
      });
  }

  createIncludeRequest(): IncludeMovementRequest | null {
    if (!this.includeEnabled()) {
      return null;
    }

    const value = this.movementForm.getRawValue();
    const monetaryValue = toApiMonetaryText(value.valor);
    if (!value.codigoProduto || !value.codigoCosif || monetaryValue === null) {
      return null;
    }

    return {
      mes: Number(value.mes),
      ano: Number(value.ano),
      codigoProduto: value.codigoProduto,
      codigoCosif: value.codigoCosif,
      descricao: value.descricao,
      valor: monetaryValue,
    };
  }

  formatValueOnBlur(): void {
    const control = this.movementForm.controls.valor;
    if (!control.disabled && monetaryTextValidator(control) === null && control.value !== '') {
      control.setValue(formatMonetaryTextForDisplay(control.value));
    }
  }

  fieldError(field: FormField): string | null {
    const control = this.movementForm.controls[field];
    if (control.disabled) {
      return null;
    }

    const submissionError = this.submissionFieldErrors()[field];
    if (submissionError) {
      return submissionError;
    }

    if (control.valid || (!control.touched && !control.dirty)) {
      return null;
    }

    if (control.hasError('required')) {
      return field === 'codigoProduto'
        ? 'Selecione um produto.'
        : field === 'codigoCosif'
          ? 'Selecione um COSIF.'
          : 'Campo obrigatório.';
    }

    if (control.hasError('integerFormat')) {
      return field === 'mes'
        ? 'Informe o mês com até dois dígitos inteiros.'
        : 'Informe o ano com até quatro dígitos inteiros.';
    }

    if (control.hasError('integerRange')) {
      return field === 'mes' ? 'Informe um mês entre 1 e 12.' : 'Informe um ano entre 1 e 9999.';
    }

    if (control.hasError('monetaryFormat')) {
      return 'Informe um valor decimal com até 16 dígitos inteiros e duas casas.';
    }

    if (control.hasError('monetaryZero')) {
      return 'O valor deve ser diferente de zero.';
    }

    if (control.hasError('whitespaceOnly')) {
      return 'A descrição deve conter ao menos um caractere diferente de espaço.';
    }

    if (control.hasError('maxlength')) {
      return 'A descrição deve ter até 50 caracteres.';
    }

    return null;
  }

  goToPreviousPage(): void {
    if (this.canGoToPreviousPage()) {
      this.pageRequests.next((this.pageData()?.page ?? 0) - 1);
    }
  }

  goToFirstPage(): void {
    if (this.canGoToPreviousPage()) {
      this.pageRequests.next(0);
    }
  }

  goToNextPage(): void {
    if (this.canGoToNextPage()) {
      this.pageRequests.next((this.pageData()?.page ?? 0) + 1);
    }
  }

  retry(): void {
    this.pageRequests.next(this.requestedPage());
  }

  formatValue(value: string): string {
    const decimalSeparator = value.indexOf('.');
    return decimalSeparator === -1
      ? value
      : `${value.slice(0, decimalSeparator)},${value.slice(decimalSeparator + 1)}`;
  }

  trackMovement(movement: ManualMovementResponse): string {
    return [
      movement.ano,
      movement.mes,
      movement.numeroLancamento,
      movement.codigoProduto,
    ].join('-');
  }

  private onProductChanged(codigoProduto: string | null): void {
    if (this.formMode() !== 'including') {
      return;
    }

    this.movementForm.controls.codigoCosif.reset(null, { emitEvent: false });
    this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
    this.cosifs.set([]);
    this.cosifErrorMessage.set(null);

    if (codigoProduto) {
      this.requestCosifs(codigoProduto);
    } else {
      this.cosifRequests.next(null);
      this.cosifsState.set('idle');
      this.updateIncludeEnabled();
    }
  }

  private requestCosifs(codigoProduto: string): void {
    this.movementForm.controls.codigoCosif.reset(null, { emitEvent: false });
    this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
    this.cosifs.set([]);
    this.cosifErrorMessage.set(null);
    this.cosifsState.set('loading');
    this.cosifRequests.next(codigoProduto);
    this.updateIncludeEnabled();
  }

  private updateIncludeEnabled(): void {
    const codigoCosif = this.movementForm.controls.codigoCosif.value;
    this.includeEnabled.set(
      this.formMode() === 'including' &&
        this.cosifsState() === 'loaded' &&
        this.movementForm.valid &&
        !!codigoCosif &&
        this.cosifs().some((cosif) => cosif.codigoCosif === codigoCosif),
    );
  }

  private resetAfterSuccessfulInclusion(): void {
    this.cosifRequests.next(null);
    this.movementForm.reset(
      {
        mes: '',
        ano: '',
        codigoProduto: null,
        codigoCosif: null,
        valor: '',
        descricao: '',
      },
      { emitEvent: false },
    );
    this.movementForm.controls.codigoProduto.setValue(null, { emitEvent: true });
    this.movementForm.disable({ emitEvent: false });
    this.formMode.set('view');
    this.cosifs.set([]);
    this.cosifsState.set('idle');
    this.cosifErrorMessage.set(null);
    this.includeErrorMessage.set(null);
    this.submissionFieldErrors.set({});
    this.updateIncludeEnabled();
  }

  private restoreAfterInclusionFailure(error: unknown): void {
    this.formMode.set('including');
    this.movementForm.enable({ emitEvent: false });
    if (this.cosifsState() !== 'loaded') {
      this.movementForm.controls.codigoCosif.disable({ emitEvent: false });
    }

    const problem = this.getSubmissionProblem(error);
    this.submissionFieldErrors.set(problem.fieldErrors);
    this.includeErrorMessage.set(problem.detail);
    this.updateIncludeEnabled();
  }

  private getSubmissionProblem(error: unknown): {
    detail: string;
    fieldErrors: Partial<Record<FormField, string>>;
  } {
    const body =
      error instanceof HttpErrorResponse &&
      typeof error.error === 'object' &&
      error.error !== null
        ? (error.error as { detail?: unknown; errors?: unknown })
        : null;
    const fieldErrors: Partial<Record<FormField, string>> = {};
    const knownFields: FormField[] = [
      'mes',
      'ano',
      'codigoProduto',
      'codigoCosif',
      'valor',
      'descricao',
    ];

    if (Array.isArray(body?.errors)) {
      for (const item of body.errors) {
        if (typeof item !== 'object' || item === null) {
          continue;
        }

        const field = (item as { field?: unknown }).field;
        const message = (item as { message?: unknown }).message;
        if (
          typeof field === 'string' &&
          knownFields.includes(field as FormField) &&
          typeof message === 'string' &&
          message.length > 0
        ) {
          fieldErrors[field as FormField] = message;
        }
      }
    }

    return {
      detail:
        typeof body?.detail === 'string' && body.detail.trim().length > 0
          ? body.detail
          : 'Não foi possível incluir o movimento. Revise os dados e tente novamente.',
      fieldErrors,
    };
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const detail = (error.error as { detail?: unknown } | null)?.detail;
      if (typeof detail === 'string' && detail.trim().length > 0) {
        return detail;
      }
    }

    return 'Não foi possível carregar os movimentos. Tente novamente.';
  }
}
