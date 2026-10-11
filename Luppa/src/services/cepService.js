import { cepDigits } from '../utils/validation'

const VIA_CEP_BASE_URL = 'https://viacep.com.br/ws'

export const lookupBrazilianCep = async (cep, signal) => {
  const digits = cepDigits(cep)
  if (digits.length !== 8) {
    throw new Error('invalid_cep')
  }

  const response = await fetch(`${VIA_CEP_BASE_URL}/${digits}/json/`, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    throw new Error('cep_lookup_failed')
  }

  const payload = await response.json()

  if (payload.erro) {
    return null
  }

  return {
    cep: digits,
    rua: payload.logradouro || '',
    bairro: payload.bairro || '',
    cidade: payload.localidade || '',
    estado: payload.uf || '',
    pais: 'Brasil',
  }
}
