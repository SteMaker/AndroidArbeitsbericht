package com.stemaker.arbeitsbericht.data.base

import java.beans.PropertyChangeEvent

/* T is the type of the element
 * source is the DataObject or the ElementList holding that element
 * e is the element that changed
 */
class ElementChangeEvent<T1: DataElement, T2: DataSimple<*>>(source: T1, val e: T2): PropertyChangeEvent(source, e.elementName, null, null)