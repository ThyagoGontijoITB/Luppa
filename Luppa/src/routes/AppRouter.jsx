import {
    BrowserRouter,
    Routes,
    Route
  
}
    from "react-router-dom"
import HomeLuppa from "../pages/HomeLuppa/HomeLuppa"

const  AppRoutes = () => {
    return (
        <BrowserRouter>
        <Routes>
            <Route path="/" element={<HomeLuppa/>}/>
            </Routes>
        </BrowserRouter>
    )
}
export default AppRoutes