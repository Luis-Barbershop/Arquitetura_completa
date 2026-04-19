import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  isLoggedIn,
  isCustomer,
  isBarber,
  isOwnerUser,
} from '../services/userContext';

export const useAuthGuard = ({
  allowCustomer = false,
  allowBarber = true,
  requireOwner = false,
  redirectIfUnauth = '/',
  redirectIfCustomerDenied = '/homepage',
  redirectIfBarberDenied = '/barberHome',
  redirectIfOwnerDenied = '/barberHome',
} = {}) => {
  const navigate = useNavigate();
  const [isAuthorized, setIsAuthorized] = useState(false);

  useEffect(() => {
    if (!isLoggedIn()) {
      setIsAuthorized(false);
      navigate(redirectIfUnauth, { replace: true });
      return;
    }

    if (isCustomer() && !allowCustomer) {
      setIsAuthorized(false);
      navigate(redirectIfCustomerDenied, { replace: true });
      return;
    }

    if (isBarber() && !allowBarber) {
      setIsAuthorized(false);
      navigate(redirectIfBarberDenied, { replace: true });
      return;
    }

    if (requireOwner && !isOwnerUser()) {
      setIsAuthorized(false);
      navigate(redirectIfOwnerDenied, { replace: true });
      return;
    }

    setIsAuthorized(true);
  }, [
    allowBarber,
    allowCustomer,
    navigate,
    redirectIfBarberDenied,
    redirectIfCustomerDenied,
    redirectIfOwnerDenied,
    redirectIfUnauth,
    requireOwner,
  ]);

  return { isAuthorized };
};
