export function swipePage(page, dx, dy, dragging = false) {
  if (dragging || Math.abs(dx) < 60 || Math.abs(dx) < Math.abs(dy) * 1.25) return page;
  return Math.max(1, Math.min(3, page + (dx < 0 ? 1 : -1)));
}
