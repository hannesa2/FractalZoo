package com.draabek.fractal.util

import java.util.*

/**
 * Low performance and low capacity tree structure
 *
 * @param <T> The datatype this tree holds
</T> */
class SimpleTree<T>(data: T) {
    @JvmField
    var data: T
    private var parent: SimpleTree<T>? = null
    @JvmField
    var children: MutableList<SimpleTree<T>>
    val isRoot: Boolean
        get() = parent == null
    val isLeaf: Boolean
        get() = children.size == 0

    init {
        this.data = data
        children = LinkedList<SimpleTree<T>>()
    }

    fun addChild(child: T): SimpleTree<T> {
        val childNode = SimpleTree(child)
        childNode.parent = this
        children.add(childNode)
        return childNode
    }

    val level: Int
        get() = if (isRoot) 0 else parent!!.level + 1

    override fun toString(): String {
        return if (data != null) data.toString() else "[null]"
    }

    fun putPath(pathParam: Deque<T>, value: T) {
        if (pathParam.isEmpty() || pathParam.peekLast() != data) {
            return
        }
        val path: Deque<T> = ArrayDeque(pathParam)
        path.removeLast()
        if (path.isEmpty()) {
            addChild(value)
            return
        }
        for (repeat in 0..1) {
            for (i in children.indices) {
                val child = children[i]
                if (path.peekLast() != null && path.peekLast() == child.data) {
                    child.putPath(path, value)
                    return
                }
            }
            addChild(path.peekLast())
        }
    }

    fun putPath(pathParam: Array<T>, value: T) {
        val l = mutableListOf(*pathParam)
        l.reverse()
        val path = ArrayDeque(l)
        putPath(path, value)
    }

    private fun enumerateChildren(): List<T> {
        val childrenData: MutableList<T> = ArrayList()
        for (child in children) {
            childrenData.add(child.data)
        }
        return childrenData
    }

    fun getChildren(pathParam: Array<T>): List<T>? {
        val l = mutableListOf(*pathParam)
        l.reverse()
        val path = ArrayDeque(l)
        return getChildren(path)
    }

    fun getChildren(pathParam: Deque<T>?): List<T>? {
        val path: Deque<T> = ArrayDeque(pathParam)
        if (path.size > 0) {
            for (child in children) {
                if (path.peekLast() == child.data) {
                    path.removeLast()
                    return child.getChildren(path)
                }
            }
            return null
        }
        return enumerateChildren()
    }
}