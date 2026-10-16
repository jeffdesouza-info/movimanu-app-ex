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

export interface ProductResponse {
  codigoProduto: string;
  descricaoProduto: string | null;
}

export interface CosifResponse {
  codigoCosif: string;
  codigoClassificacao: string | null;
}

export interface IncludeMovementRequest {
  mes: number;
  ano: number;
  codigoProduto: string;
  codigoCosif: string;
  descricao: string;
  valor: string;
}
