import { useState } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';
import { listAudit } from '../api/audit';
import type { AuditEntry } from '../api/types';
import { formatDateTime, outcomeLabel } from '../i18n/labels';

export function AuditPage() {
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 25 });

  const query = useQuery({
    queryKey: ['audit', paginationModel],
    queryFn: () => listAudit(paginationModel.page, paginationModel.pageSize),
    placeholderData: keepPreviousData,
  });

  const columns: GridColDef<AuditEntry>[] = [
    {
      field: 'at',
      headerName: 'Zaman',
      width: 190,
      valueFormatter: (value) => formatDateTime(value as string),
    },
    { field: 'action', headerName: 'İşlem', width: 160 },
    {
      field: 'outcome',
      headerName: 'Sonuç',
      width: 120,
      renderCell: (params) => (
        <Chip
          size="small"
          label={outcomeLabel(params.value as string)}
          color={params.value === 'SUCCESS' ? 'success' : 'error'}
          variant="outlined"
        />
      ),
    },
    { field: 'username', headerName: 'Kullanıcı', width: 120 },
    { field: 'entityType', headerName: 'Varlık', width: 110 },
    { field: 'entityId', headerName: 'Varlık no', flex: 1, minWidth: 180 },
    { field: 'ipAddress', headerName: 'IP', width: 130 },
  ];

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Denetim kaydı</Typography>
      <Paper sx={{ height: 600 }}>
        <DataGrid
          rows={query.data?.content ?? []}
          columns={columns}
          getRowId={(row) => row.id}
          loading={query.isLoading || query.isFetching}
          rowCount={query.data?.totalElements ?? 0}
          paginationMode="server"
          paginationModel={paginationModel}
          onPaginationModelChange={setPaginationModel}
          pageSizeOptions={[25, 50, 100]}
          disableRowSelectionOnClick
          disableColumnMenu
          sx={{ border: 0 }}
        />
      </Paper>
    </Stack>
  );
}
