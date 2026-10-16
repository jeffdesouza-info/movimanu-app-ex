import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CosifResponse,
  ManualMovementPageResponse,
  ProductResponse,
} from './manual-movements.models';

@Injectable({ providedIn: 'root' })
export class ManualMovementsApiService {
  private readonly http = inject(HttpClient);
  private readonly movementsUrl = `${environment.apiBaseUrl}/movimentos-manuais`;

  listPage(page: number): Observable<ManualMovementPageResponse> {
    const params = new HttpParams().set('page', page);
    return this.http.get<ManualMovementPageResponse>(this.movementsUrl, { params });
  }

  listProducts(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${environment.apiBaseUrl}/produtos`);
  }

  listCosifs(codigoProduto: string): Observable<CosifResponse[]> {
    const encodedProductCode = encodeURIComponent(codigoProduto);
    return this.http.get<CosifResponse[]>(
      `${environment.apiBaseUrl}/produtos/${encodedProductCode}/cosifs`,
    );
  }
}
