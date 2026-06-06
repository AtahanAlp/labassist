import { useState } from 'react';
import axios from 'axios';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Stack from '@mui/material/Stack';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import { listUsers, createUser } from '../api/users';
import type { UserRole } from '../api/types';
import { roleLabel } from '../i18n/labels';

export function UsersPage() {
  const queryClient = useQueryClient();
  const usersQuery = useQuery({ queryKey: ['users'], queryFn: listUsers });

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [role, setRole] = useState<UserRole>('DOCTOR');

  const mutation = useMutation({
    mutationFn: () => createUser({ username, password, displayName: displayName || undefined, role }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      setUsername('');
      setPassword('');
      setDisplayName('');
      setRole('DOCTOR');
    },
  });

  const errorMessage = mutation.isError
    ? axios.isAxiosError(mutation.error) && mutation.error.response?.status === 409
      ? 'Bu kullanıcı adı zaten alınmış.'
      : 'Hesap oluşturulamadı. Şifre en az 8 karakter olmalı.'
    : null;

  return (
    <Stack spacing={2}>
      <Typography variant="h5">Kullanıcılar</Typography>

      <Paper sx={{ p: 2.5 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Hesap oluştur
        </Typography>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            mutation.mutate();
          }}
        >
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ flexWrap: 'wrap', rowGap: 2 }}>
            <TextField label="Kullanıcı adı" size="small" value={username} onChange={(e) => setUsername(e.target.value)} required />
            <TextField label="Şifre" type="password" size="small" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <TextField label="Görünen ad" size="small" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            <TextField select label="Rol" size="small" value={role} onChange={(e) => setRole(e.target.value as UserRole)} sx={{ minWidth: 130 }}>
              <MenuItem value="DOCTOR">Doktor</MenuItem>
              <MenuItem value="ADMIN">Yönetici</MenuItem>
            </TextField>
            <Button type="submit" variant="contained" disabled={mutation.isPending || !username || password.length < 8}>
              {mutation.isPending ? 'Oluşturuluyor' : 'Oluştur'}
            </Button>
          </Stack>
        </form>
        {errorMessage && <Alert severity="error" sx={{ mt: 2 }}>{errorMessage}</Alert>}
        {mutation.isSuccess && <Alert severity="success" sx={{ mt: 2 }}>Hesap oluşturuldu.</Alert>}
      </Paper>

      <Paper sx={{ p: 0, overflow: 'hidden' }}>
        <Typography variant="h6" sx={{ p: 2, pb: 1 }}>
          Hesaplar
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Kullanıcı adı</TableCell>
              <TableCell>Görünen ad</TableCell>
              <TableCell>Rol</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(usersQuery.data ?? []).map((u) => (
              <TableRow key={u.username}>
                <TableCell>{u.username}</TableCell>
                <TableCell>{u.displayName ?? '-'}</TableCell>
                <TableCell>
                  <Chip size="small" label={roleLabel[u.role]} variant="outlined" />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Stack>
  );
}
