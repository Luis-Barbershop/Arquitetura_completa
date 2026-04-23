export const DIGITS_ONLY_REGEX = /\D/g;

export function onlyDigits(value = '') {
    return String(value).replace(DIGITS_ONLY_REGEX, '');
}

export function maskCpf(value = '') {
    const digits = onlyDigits(value).slice(0, 11);

    return digits
        .replace(/(\d{3})(\d)/, '$1.$2')
        .replace(/(\d{3})(\d)/, '$1.$2')
        .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

export function maskPhone(value = '') {
    const digits = onlyDigits(value).slice(0, 11);

    if (!digits) return '';
    if (digits.length <= 2) return `(${digits}`;
    if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
    if (digits.length <= 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;

    return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
}
