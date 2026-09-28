import { Navigate, type RouteObject } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { NotFoundPage } from './components/NotFoundPage'
import { DocumentDetailsPage } from './features/documents/DocumentDetailsPage'
import { DocumentsPage } from './features/documents/DocumentsPage'
import { UploadPage } from './features/documents/UploadPage'

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/documents" replace /> },
      { path: 'documents', element: <DocumentsPage /> },
      { path: 'documents/upload', element: <UploadPage /> },
      { path: 'documents/:id', element: <DocumentDetailsPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
