import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ManualMovementPageResponse } from './manual-movements.models';

@Injectable({ providedIn: 'root' })
export class ManualMovementsApiService {
  private readonly http = inject(HttpClient);
  private readonly movementsUrl = `${environment.apiBaseUrl}/movimentos-manuais`;

  listPage(page: number): Observable<ManualMovementPageResponse> {
    const params = new HttpParams().set('page', page);
    return this.http.get<ManualMovementPageResponse>(this.movementsUrl, { params });
  }
}
