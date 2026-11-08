import { api } from './client'
import { crud } from './crud'
import type {
  Asset,
  AssetAssignRequest,
  AssetAssignment,
  AssetRequest,
  AssetReturnRequest,
  EmployeeDocument,
  EmployeeDocumentRequest,
} from './types'

export const assetsApi = crud<Asset, AssetRequest>('/api/assets')
export const documentsApi = crud<EmployeeDocument, EmployeeDocumentRequest>('/api/documents')

const assignments = '/api/asset-assignments'
export const assetAssignmentsApi = {
  list: (params?: Record<string, unknown>) => api.get<AssetAssignment[]>(assignments, { params }).then((r) => r.data),
  assign: (body: AssetAssignRequest) => api.post<AssetAssignment>(assignments, body).then((r) => r.data),
  returnAsset: (id: number, body: AssetReturnRequest) =>
    api.post<AssetAssignment>(`${assignments}/${id}/return`, body).then((r) => r.data),
}
