import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Chip from '@mui/material/Chip';
import { listReports, getReportsSummary } from '../api/reports';
import type { LabReportSummary, ReportStatus } from '../api/types';
import { StatusChip } from '../components/StatusChip';
import { useAuth } from '../auth/AuthContext';

function StatCard({ label, value, color }: { label: string; value: number | undefined; color?: string }) {
  return (
    <Paper sx={{ p: 2, flex: 1, minWidth: 120 }}>
      <Typography variant="h4" sx={{ color, fontWeight: 700 }}>
        {value ?? '—'}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Paper>
  );
}

export function ReportsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [abnormalOnly, setAbnormalOnly] = useState(false);
  const [criticalOnly, setCriticalOnly] = useState(false);
  const [status, setStatus] = useState<ReportStatus | 'ALL'>('ALL');
  const [q, setQ] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const resetPage = () => setPaginationModel((m) => ({ ...m, page: 0 }));

  const statusOptions: Array<ReportStatus | 'ALL'> = isAdmin
    ? ['ALL', 'VALIDATED', 'PARTIAL', 'REJECTED']
    : ['ALL', 'VALIDATED', 'PARTIAL'];

  const summary = useQuery({ queryKey: ['reports-summary'], queryFn: getReportsSummary });

  const query = useQuery({
    queryKey: ['reports', paginationModel, abnormalOnly, criticalOnly, status, q, from, to],
    queryFn: () =>
      listReports({
        page: paginationModel.page,
        size: paginationModel.pageSize,
        abnormalOnly: abnormalOnly || undefined,
        criticalOnly: criticalOnly || undefined,
        status: status === 'ALL' ? undefined : status,
        q: q.trim() || undefined,
        from: from || undefined,
        to: to || undefined,
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

      <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', rowGap: 2 }}>
        <StatCard label="Reports" value={summary.data?.total} />
        <StatCard label="With abnormal values" value={summary.data?.abnormal} color="warning.main" />
        <StatCard label="With critical values" value={summary.data?.critical} color="error.main" />
        {isAdmin && <StatCard label="Rejected (malformed)" value={summary.data?.rejected} color="text.secondary" />}
      </Stack>

      <Paper sx={{ p: 2 }}>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ flexWrap: 'wrap', rowGap: 2 }}>
          <TextField
            label="Search report id"
            size="small"
            value={q}
            onChange={(e) => {
              setQ(e.target.value);
              resetPage();
            }}
          />
          <TextField
            select
            label="Status"
            size="small"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as ReportStatus | 'ALL');
              resetPage();
            }}
            sx={{ minWidth: 140 }}
          >
            {statusOptions.map((option) => (
              <MenuItem key={option} value={option}>
                {option}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="From"
            type="date"
            size="small"
            value={from}
            onChange={(e) => {
              setFrom(e.target.value);
              resetPage();
            }}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            label="To"
            type="date"
            size="small"
            value={to}
            onChange={(e) => {
              setTo(e.target.value);
              resetPage();
            }}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={abnormalOnly}
                onChange={(e) => {
                  setAbnormalOnly(e.target.checked);
                  resetPage();
                }}
              />
            }
            label="Abnormal only"
          />
          <FormControlLabel
            control={
              <Switch
                checked={criticalOnly}
                onChange={(e) => {
                  setCriticalOnly(e.target.checked);
                  resetPage();
                }}
              />
            }
            label="Critical only"
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
    </Stack>
  );
}
