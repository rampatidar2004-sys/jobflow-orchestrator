export default function PriorityBadge({ priority }) {
  return <span className="priority-badge" title={`priority ${priority}`}>{priority}</span>;
}
