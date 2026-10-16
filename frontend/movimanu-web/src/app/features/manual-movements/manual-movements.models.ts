export interface ManualMovementResponse {
  mes: number;
  ano: number;
  codigoProduto: string;
  descricaoProduto: string | null;
  numeroLancamento: string;
  descricao: string;
  valor: string;
}

export interface ManualMovementPageResponse {
  content: ManualMovementResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
