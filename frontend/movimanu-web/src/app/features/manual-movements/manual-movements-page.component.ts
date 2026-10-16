import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, Subject, switchMap, tap } from 'rxjs';
import { ManualMovementsApiService } from './manual-movements-api.service';
import {
  ManualMovementPageResponse,
  ManualMovementResponse,
} from './manual-movements.models';

type QueryState = 'loading' | 'loaded' | 'error';

type QueryResult =
  | { kind: 'loaded'; page: ManualMovementPageResponse }
  | { kind: 'error'; error: unknown };

@Component({
  imports: [],
  selector: 'app-manual-movements-page',
  styleUrl: './manual-movements-page.component.css',
  templateUrl: './manual-movements-page.component.html',
})
export class ManualMovementsPageComponent {
  private readonly movementsApi = inject(ManualMovementsApiService);
  private readonly pageRequests = new Subject<number>();

  readonly queryState = signal<QueryState>('loading');
  readonly pageData = signal<ManualMovementPageResponse | null>(null);
  readonly requestedPage = signal(0);
  readonly errorMessage = signal<string | null>(null);

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
