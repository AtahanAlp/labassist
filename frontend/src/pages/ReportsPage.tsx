import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Chip from '@mui/material/Chip';
import { listReports } from '../api/reports';
import type { LabReportSummary, ReportStatus } from '../api/types';
import { StatusChip } from '../components/StatusChip';

const STATUS_OPTIONS: Array<ReportStatus | 'ALL'> = ['ALL', 'VALIDATED', 'PARTIAL', 'REJECTED'];

export function ReportsPage() {
  const navigate = useNavigate();
  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [abnormalOnly, setAbnormalOnly] = useState(false);
  const [status, setStatus] = useState<ReportStatus | 'ALL'>('ALL');
  const [q, setQ] = useState('');

  const query = useQuery({
    queryKey: ['reports', paginationModel, abnormalOnly, status, q],
    queryFn: () =>
      listReports({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        abnormalOnly: abnormalOnly || undefined,
        status: status === 'ALL' ? undefined : status,
        q: q.trim() || undefined,
      }),
    placeholderData: keepPreviousData,
  });

  const columns = useMemo<GridColDef<LabReportSummary>[]>(
    () => [
      {
        field: 'receivedAt',
        headerName: 'Received',
        width: 170,
        valueFormatter: (value) => (value ? new Date(value as string).toLocaleString() : ''),
      },
      { field: 'externalId', headerName: 'Report', width: 120 },
      { field: 'patientName', headerName: 'Patient', flex: 1, minWidth: 140 },
      {
        field: 'patient',
        headerName: 'Age / Sex',
        width: 110,
        sortable: false,
        valueGetter: (_value, row) => `${row.patientAge ?? '?'} / ${row.patientSex ?? '?'}`,
      },
      { field: 'deviceId', headerName: 'Device', width: 120 },
      {
        field: 'status',
        headerName: 'Status',
        width: 130,
        renderCell: (params) => <StatusChip status={params.value as ReportStatus} />,
      },
      {
        field: 'abnormalCount',
        headerName: 'Abnormal',
        width: 110,
        renderCell: (params) =>
          params.value > 0 ? <Chip size="small" color="warning" label={params.value} /> : <span>—</span>,
      },
      {
        field: 'criticalCount',
        headerName: 'Critical',
        width: 100,
        renderCell: (params) =>
          params.value > 0 ? <Chip size="small" color="error" label={params.value} /> : <span>—</span>,
      },
    ],
    [],
  );

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Lab Reports</Typography>

      <Paper sx={{ p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
          <TextField
            label="Search report id"
            size="small"
            value={q}
            onChange={(e) => {
              setQ(e.target.value);
              setPaginationModel((m) => ({ ...m, page: 0 }));
            }}
          />
          <TextField
            select
            label="Status"
            size="small"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as ReportStatus | 'ALL');
              setPaginationModel((m) => ({ ...m, page: 0 }));
            }}
            sx={{ minWidth: 150 }}
          >
            {STATUS_OPTIONS.map((option) => (
              <MenuItem key={option} value={option}>
                {option}
              </MenuItem>
            ))}
          </TextField>
          <FormControlLabel
            control={
              <Switch
                checked={abnormalOnly}
                onChange={(e) => {
                  setAbnormalOnly(e.target.checked);
                  setPaginationModel((m) => ({ ...m, page: 0 }));
                }}
              />
            }
            label="Abnormal only"
          />
        </Stack>
      </Paper>

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
          pageSizeOptions={[10, 20, 50]}
          disableRowSelectionOnClick
          disableColumnMenu
          onRowClick={(params) => navigate(`/reports/${params.id}`)}
          getRowClassName={(params) =>
            params.row.criticalCount > 0 ? 'row-critical' : params.row.overallAbnormal ? 'row-abnormal' : ''
          }
          sx={{
            border: 0,
            '& .MuiDataGrid-row': { cursor: 'pointer' },
            '& .row-critical': { bgcolor: 'rgba(198,40,40,0.08)' },
            '& .row-abnormal': { bgcolor: 'rgba(239,108,0,0.07)' },
          }}
        />
      </Paper>
      <Box />
    </Stack>
  );
}
