import Paper from '@mui/material/Paper';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Stack from '@mui/material/Stack';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import ReplayIcon from '@mui/icons-material/Replay';
import ReactMarkdown from 'react-markdown';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getInterpretation, requestInterpretation } from '../api/reports';
import type { LlmInterpretation } from '../api/types';
import { formatDateTime } from '../i18n/labels';

export function AiInterpretationPanel({ reportId }: { reportId: string }) {
  const queryClient = useQueryClient();

  const existing = useQuery({
    queryKey: ['interpretation', reportId],
    queryFn: () => getInterpretation(reportId),
  });

  const mutation = useMutation({
    mutationFn: (refresh: boolean) => requestInterpretation(reportId, refresh),
    onSuccess: (data) => queryClient.setQueryData(['interpretation', reportId], data),
  });

  const interpretation: LlmInterpretation | null | undefined = mutation.data ?? existing.data;
  const loading = mutation.isPending;

  return (
    <Paper sx={{ p: 2.5 }}>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1.5 }}>
        <AutoAwesomeIcon color="primary" />
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          Yapay zeka ön değerlendirmesi
        </Typography>
        {interpretation?.model && <Chip size="small" label={interpretation.model} />}
      </Stack>

      <Alert severity="info" sx={{ mb: 2 }}>
        Bu, incelemenizi desteklemek için yapay zeka tarafından üretilmiş bir ön değerlendirmedir.{' '}
        <strong>Tanı değildir</strong> ve bir hekim tarafından onaylanmalıdır.
        <Box component="span" sx={{ display: 'block', mt: 0.5 }}>
          <strong>Gizlilik:</strong> yapay zekaya yalnızca kimliksizleştirilmiş veriler (yaş,
          cinsiyet, analit değerleri, referans aralıkları ve değerlendirmeler) gönderilir. Hastanın
          adı ve hasta numarası asla paylaşılmaz.
        </Box>
      </Alert>

      {mutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Yapay zeka asistanı şu anda kullanılamıyor. Lütfen tekrar deneyin.
        </Alert>
      )}

      {loading && (
        <Stack direction="row" spacing={2} alignItems="center" sx={{ py: 3 }}>
          <CircularProgress size={22} />
          <Typography color="text.secondary">
            Değerlendirme oluşturuluyor. Bu işlem yerel olarak çalışır ve 10 ila 60 saniye sürebilir.
          </Typography>
        </Stack>
      )}

      {!loading && interpretation?.responseText && (
        <Box sx={{ '& p': { mt: 0.5, mb: 1 }, '& ul': { mt: 0.5, mb: 1, pl: 3 } }}>
          <ReactMarkdown>{interpretation.responseText}</ReactMarkdown>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 2 }}>
            Oluşturan: {interpretation.createdBy ?? 'sistem'}
            {interpretation.latencyMs != null && ` · ${(interpretation.latencyMs / 1000).toFixed(1)} sn`}
            {interpretation.createdAt && ` · ${formatDateTime(interpretation.createdAt)}`}
          </Typography>
        </Box>
      )}

      {!loading && (
        <Box sx={{ mt: 1 }}>
          <Button
            variant={interpretation ? 'outlined' : 'contained'}
            startIcon={interpretation ? <ReplayIcon /> : <AutoAwesomeIcon />}
            onClick={() => mutation.mutate(Boolean(interpretation))}
            disabled={existing.isLoading}
          >
            {interpretation ? 'Yeniden oluştur' : 'Yapay zeka yorumu al'}
          </Button>
        </Box>
      )}
    </Paper>
  );
}
