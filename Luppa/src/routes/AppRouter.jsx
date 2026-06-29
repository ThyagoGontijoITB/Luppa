import { BrowserRouter, Route, Routes } from 'react-router-dom'
import CadastroEventoLuppa from '../pages/CadastroEventoLuppa/CadastroEventoLuppa'
import HomeLuppa from '../pages/HomeLuppa/HomeLuppa'
import LoginCadastroLuppa from '../pages/LoginCadastroLuppa/LoginCadastroLuppa'

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomeLuppa />} />
        <Route path="/login-cadastro" element={<LoginCadastroLuppa />} />
        <Route path="/cadastrar-evento" element={<CadastroEventoLuppa />} />
      </Routes>
    </BrowserRouter>
  )
}

export default AppRouter
