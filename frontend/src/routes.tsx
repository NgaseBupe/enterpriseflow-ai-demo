import { Navigate, type RouteObject } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { NotFoundPage } from './components/NotFoundPage'
import { DocumentsPage } from './features/documents/DocumentsPage'
import { UploadPage } from './features/documents/UploadPage'
import { DocumentReviewPage } from './features/review/DocumentReviewPage'

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/documents" replace /> },
      { path: 'documents', element: <DocumentsPage /> },
      { path: 'documents/upload', element: <UploadPage /> },
      { path: 'documents/:id', element: <DocumentReviewPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
