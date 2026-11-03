export default function StateMessage({ loading, error, empty, emptyText = 'Nothing here yet.' }) {
  if (loading) return <div className="state-message">Loading…</div>;
  if (error) return <div className="state-message error">{error}</div>;
  if (empty) return <div className="state-message">{emptyText}</div>;
  return null;
}
