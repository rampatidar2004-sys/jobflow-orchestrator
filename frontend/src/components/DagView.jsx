import StatusBadge from './StatusBadge';

/** Groups steps into dependency layers (BFS by longest-path depth) and renders them left-to-right. */
function computeLayers(steps) {
  const byKey = Object.fromEntries(steps.map((s) => [s.stepKey, s]));
  const depth = {};

  function depthOf(key, seen = new Set()) {
    if (depth[key] !== undefined) return depth[key];
    if (seen.has(key)) return 0; // guard against cycles in display-only code
    seen.add(key);
    const step = byKey[key];
    const deps = step?.dependsOn || [];
    const d = deps.length === 0 ? 0 : 1 + Math.max(...deps.map((d2) => depthOf(d2, seen)));
    depth[key] = d;
    return d;
  }

  steps.forEach((s) => depthOf(s.stepKey));
  const maxDepth = Math.max(0, ...Object.values(depth));
  const layers = Array.from({ length: maxDepth + 1 }, () => []);
  steps.forEach((s) => layers[depth[s.stepKey]].push(s));
  return layers;
}

export default function DagView({ steps, statusByStepKey }) {
  const layers = computeLayers(steps);
  return (
    <div className="dag-canvas">
      {layers.map((layer, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
          {i > 0 && <span className="dag-arrow">→</span>}
          {layer.map((step) => (
            <div key={step.stepKey} className="dag-node">
              <strong>{step.stepKey}</strong>
              <span style={{ color: 'var(--text-muted)' }}>{step.taskType}</span>
              {statusByStepKey?.[step.stepKey] && <StatusBadge status={statusByStepKey[step.stepKey]} />}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
