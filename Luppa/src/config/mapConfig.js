const fallbackMapStyle = {
  version: 8,
  sources: {
    openStreetMap: {
      type: 'raster',
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      tileSize: 256,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    },
  },
  layers: [
    {
      id: 'openstreetmap',
      type: 'raster',
      source: 'openStreetMap',
      minzoom: 0,
      maxzoom: 19,
    },
  ],
}

export const mapConfig = {
  attributionControl: true,
  maxZoom: 18,
  minZoom: 10,
  styleUrl: import.meta.env.VITE_MAP_STYLE_URL || fallbackMapStyle,
  zoom: 15,
}
