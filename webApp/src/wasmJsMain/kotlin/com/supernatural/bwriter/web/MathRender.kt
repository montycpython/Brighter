@file:OptIn(ExperimentalWasmJsInterop::class)

package com.supernatural.bwriter.web

import kotlin.JsFun
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.toJsString

// Hand-written interop to KaTeX, the browser LaTeX renderer. index.html loads
// KaTeX from the jsDelivr CDN and exposes renderToString as
// window.BwriterKatex before this module starts.
//
// The shared parser (BookMarkup in :shared) cuts book text into segments,
// including InlineMath ($...$) and DisplayMath ($$...$$). Android renders
// those segments with JLatexMath; the web build renders them here, from the
// same segment content, so one book shows the same formulas on both.

@JsFun("(tex, displayMode) => window.BwriterKatex.renderToString(tex, displayMode)")
private external fun katexRenderToString(tex: JsString, displayMode: Boolean): JsString

object MathRender {

  /** Renders one LaTeX formula to an HTML string. Display mode for $$...$$. */
  fun render(tex: String, displayMode: Boolean): String =
    katexRenderToString(tex.toJsString(), displayMode).toString()
}
