import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import Stack from '@mui/material/Stack';
import Paper from '@mui/material/Paper';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { getReport } from '../api/reports';
import type { AnalyteFlag } from '../api/types';
import { FlagChip } from '../components/FlagChip';
import { StatusChip } from '../components/StatusChip';
import { AiInterpretationPanel } from '../components/AiInterpretationPanel';
import { formatDateTime, sexShort } from '../i18n/labels';

const rowBg = (flag: AnalyteFlag) => {
  if (flag === 'CRITICAL_LOW' || flag === 'CRITICAL_HIGH') return 'rgba(198,40,40,0.08)';
  if (flag === 'LOW' || flag === 'HIGH') return 'rgba(239,108,0,0.07)';
  return undefined;
};

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2">{value ?? '-'}</Typography>
    </Box>
  );
}

export function ReportDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const { data: report, isLoading, isError } = useQuery({
    queryKey: ['report', id],
    queryFn: () => getReport(id),
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }
  if (isError || !report) {
    return <Alert severity="error">Bu rapor yüklenemedi.</Alert>;
  }

  return (
    <Stack spacing={2}>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/reports')} sx={{ alignSelf: 'flex-start' }}>
        Raporlara dön
      </Button>

      <Paper sx={{ p: 2.5 }}>
        <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 2 }}>
          <Typography variant="h5" sx={{ flexGrow: 1 }}>
            {report.patientName ?? 'Bilinmeyen hasta'}
          </Typography>
          <StatusChip status={report.status} />
        </Stack>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(3, 1fr)', md: 'repeat(4, 1fr)' },
            gap: 2,
          }}
        >
          <Field label="Rapor no" value={report.externalId} />
          <Field label="Hasta no" value={report.patientMrn} />
          <Field label="Yaş / Cinsiyet" value={`${report.patientAge ?? '?'} / ${sexShort(report.patientSex)}`} />
          <Field label="Cihaz" value={report.deviceId} />
          <Field label="Örnek alındı" value={formatDateTime(report.sampleCollectedAt)} />
          <Field label="Alındı" value={formatDateTime(report.receivedAt)} />
          <Field label="Anormal" value={report.abnormalCount} />
          <Field label="Kritik" value={report.criticalCount} />
        </Box>
        {report.status === 'REJECTED' && report.rejectionReason && (
          <Alert severity="error" sx={{ mt: 2 }}>
            Doğrulama sırasında reddedildi: {report.rejectionReason}
          </Alert>
        )}
      </Paper>

      {report.tests.length > 0 && (
        <Paper sx={{ p: 0, overflow: 'hidden' }}>
          <Typography variant="h6" sx={{ p: 2, pb: 1 }}>
            Analitler
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Kod</TableCell>
                <TableCell>Test</TableCell>
                <TableCell align="right">Değer</TableCell>
                <TableCell>Birim</TableCell>
                <TableCell align="right">Referans aralığı</TableCell>
                <TableCell>Değerlendirme</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {report.tests.map((test) => (
                <TableRow key={test.code} sx={{ bgcolor: rowBg(test.flag) }}>
                  <TableCell>{test.code}</TableCell>
                  <TableCell>{test.name ?? '-'}</TableCell>
                  <TableCell align="right">
                    <strong>{test.value ?? 'ölçülmedi'}</strong>
                  </TableCell>
                  <TableCell>{test.unit ?? ''}</TableCell>
                  <TableCell align="right">
                    {test.refLow ?? '-'} - {test.refHigh ?? '-'}
                  </TableCell>
                  <TableCell>
                    <FlagChip flag={test.flag} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}

      {report.status !== 'REJECTED' && report.tests.length > 0 && <AiInterpretationPanel reportId={report.id} />}
    </Stack>
  );
}
