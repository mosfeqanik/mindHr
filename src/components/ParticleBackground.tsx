import { useEffect, useRef } from 'react'
import * as THREE from 'three'

export type LoginTheme = 'light' | 'dark'

/** Reads a color token from styles/colors.css, so the particles follow the same palette as the CSS. */
function token(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function palette(t: LoginTheme) {
  const d = t === 'dark' ? '-dark' : ''
  return {
    p1: token(`--mh-particle${d}-1`),
    p2: token(`--mh-particle${d}-2`),
    p3: token(`--mh-particle${d}-3`),
    line: token(`--mh-particle${d}-line`),
    ...(t === 'dark'
      ? { opacity1: 0.75, opacity2: 0.5, lineOpacity: 0.12 }
      : { opacity1: 0.55, opacity2: 0.35, lineOpacity: 0.08 }),
  }
}
type Palette = ReturnType<typeof palette>

/** Random point on a sphere shell between rMin and rMin + rSpread. */
function spherePoint(rMin: number, rSpread: number): [number, number, number] {
  const r = rMin + Math.random() * rSpread
  const theta = Math.random() * Math.PI * 2
  const phi = Math.acos(Math.random() * 2 - 1)
  return [r * Math.sin(phi) * Math.cos(theta), r * Math.sin(phi) * Math.sin(theta), r * Math.cos(phi)]
}

/** Soft round sprite so points render as glowing dots instead of squares. */
function makeSprite() {
  const size = 64
  const c = document.createElement('canvas')
  c.width = c.height = size
  const ctx = c.getContext('2d')!
  const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
  g.addColorStop(0, 'rgba(255,255,255,1)')
  g.addColorStop(0.25, 'rgba(255,255,255,0.85)')
  g.addColorStop(0.55, 'rgba(255,255,255,0.25)')
  g.addColorStop(1, 'rgba(255,255,255,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  return new THREE.CanvasTexture(c)
}

/** Fills a colour buffer; the near layer mixes all three colours, the far layer cyan to violet. */
function paintColors(attr: THREE.BufferAttribute, p: Palette, near: boolean) {
  const c1 = new THREE.Color(p.p1)
  const c2 = new THREE.Color(p.p2)
  const c3 = new THREE.Color(p.p3)
  for (let i = 0; i < attr.count; i++) {
    const t = Math.random()
    let mixed: THREE.Color
    if (!near) mixed = c3.clone().lerp(c2, Math.random())
    else if (t < 0.45) mixed = c1.clone().lerp(c2, Math.random())
    else if (t < 0.8) mixed = c1.clone().lerp(c3, Math.random())
    else mixed = c2.clone().lerp(c3, Math.random())
    attr.setXYZ(i, mixed.r, mixed.g, mixed.b)
  }
  attr.needsUpdate = true
}

function makeLayer(count: number, rMin: number, rSpread: number, size: number, sprite: THREE.Texture) {
  const geo = new THREE.BufferGeometry()
  const pos = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) pos.set(spherePoint(rMin, rSpread), i * 3)
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(new Float32Array(count * 3), 3))
  const mat = new THREE.PointsMaterial({
    size,
    map: sprite,
    vertexColors: true,
    transparent: true,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    sizeAttenuation: true,
  })
  return new THREE.Points(geo, mat)
}

/** Constellation-style short line segments. */
function makeLines(count: number) {
  const pos = new Float32Array(count * 6)
  for (let i = 0; i < count; i++) {
    const [x, y, z] = spherePoint(18, 40)
    const offset = 4 + Math.random() * 8
    pos.set([x, y, z, x + (Math.random() - 0.5) * offset, y + (Math.random() - 0.5) * offset, z + (Math.random() - 0.5) * offset], i * 6)
  }
  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
  const mat = new THREE.LineBasicMaterial({ transparent: true, blending: THREE.AdditiveBlending, depthWrite: false })
  return new THREE.LineSegments(geo, mat)
}

/**
 * Full-screen Three.js particle field that follows the pointer with a slight parallax.
 * Skipped entirely when the user prefers reduced motion.
 */
export function ParticleBackground({ theme }: { theme: LoginTheme }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const applyThemeRef = useRef<((t: LoginTheme) => void) | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    let renderer: THREE.WebGLRenderer
    try {
      renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true, powerPreference: 'high-performance' })
    } catch {
      return // no WebGL: the gradient background alone is fine
    }
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(window.innerWidth, window.innerHeight)
    renderer.setClearColor(0x000000, 0)

    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000)
    camera.position.z = 32

    const small = window.innerWidth < 768
    const sprite = makeSprite()
    const near = makeLayer(small ? 350 : 700, 22, 45, 1.6, sprite)
    const far = makeLayer(small ? 200 : 400, 55, 60, 0.9, sprite)
    const lines = makeLines(small ? 60 : 110)
    scene.add(near, far, lines)

    applyThemeRef.current = (t) => {
      const p = palette(t)
      paintColors(near.geometry.getAttribute('color') as THREE.BufferAttribute, p, true)
      paintColors(far.geometry.getAttribute('color') as THREE.BufferAttribute, p, false)
      near.material.opacity = p.opacity1
      far.material.opacity = p.opacity2
      lines.material.color.set(p.line)
      lines.material.opacity = p.lineOpacity
    }
    applyThemeRef.current(theme)

    let mouseX = 0
    let mouseY = 0
    let targetX = 0
    let targetY = 0
    const onPointerMove = (e: PointerEvent) => {
      mouseX = (e.clientX / window.innerWidth) * 2 - 1
      mouseY = -((e.clientY / window.innerHeight) * 2 - 1)
    }
    const onResize = () => {
      camera.aspect = window.innerWidth / window.innerHeight
      camera.updateProjectionMatrix()
      renderer.setSize(window.innerWidth, window.innerHeight)
    }

    const timer = new THREE.Timer()
    let frame: number | null = null
    const animate = () => {
      frame = requestAnimationFrame(animate)
      timer.update()
      const t = timer.getElapsed()
      targetX += (mouseX * 0.6 - targetX) * 0.04
      targetY += (mouseY * 0.6 - targetY) * 0.04

      near.rotation.set(Math.sin(t * 0.15) * 0.15 + targetY * 0.2, t * 0.06 + targetX * 0.25, 0)
      near.position.set(targetX * 1.5, targetY * 1.5, 0)
      far.rotation.set(Math.cos(t * 0.1) * 0.1 + targetY * 0.12, -t * 0.03 + targetX * 0.15, 0)
      far.position.set(targetX * 0.8, targetY * 0.8, 0)
      lines.rotation.set(Math.sin(t * 0.12) * 0.1 + targetY * 0.15, t * 0.05 + targetX * 0.2, 0)
      lines.position.set(targetX * 1.2, targetY * 1.2, 0)

      camera.position.x = Math.sin(t * 0.1) * 1.2
      camera.position.y = Math.cos(t * 0.08) * 0.8
      camera.lookAt(0, 0, 0)
      renderer.render(scene, camera)
    }
    const stop = () => {
      if (frame != null) cancelAnimationFrame(frame)
      frame = null
    }
    // Pause while the tab is hidden to save CPU and battery.
    const onVisibility = () => (document.hidden ? stop() : frame == null && animate())

    window.addEventListener('resize', onResize, { passive: true })
    window.addEventListener('pointermove', onPointerMove, { passive: true })
    document.addEventListener('visibilitychange', onVisibility)
    animate()

    return () => {
      stop()
      window.removeEventListener('resize', onResize)
      window.removeEventListener('pointermove', onPointerMove)
      document.removeEventListener('visibilitychange', onVisibility)
      applyThemeRef.current = null
      for (const obj of [near, far, lines]) {
        obj.geometry.dispose()
        obj.material.dispose()
      }
      sprite.dispose()
      renderer.dispose()
    }
    // The scene is built once; theme changes only recolour it (effect below).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    applyThemeRef.current?.(theme)
  }, [theme])

  return <canvas ref={canvasRef} className="login-bg-canvas" aria-hidden="true" />
}
