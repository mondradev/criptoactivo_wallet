package com.cryptowallet.wallet.widgets.adapters;


import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provee de una estructura base para la implementación de adaptadores para {@link
 * RecyclerView}. También ofrece funciones para administrar la colección de los datos.
 *
 * @param <TItem>       Tipo de dato de la colección.
 * @param <TViewHolder> Tipo de dato que implementa {@link RecyclerView.ViewHolder}.
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class AdapterBase<TItem, TViewHolder extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<TViewHolder> {

    /**
     * Layout del {@link RecyclerView.ViewHolder}, que representa a cada elemento de la
     * colección.
     */
    @LayoutRes
    private final int mLayoutID;

    /**
     * Vista que se visualiza cuando la colección está vacía.
     */
    private View mEmptyView;

    /**
     * Colección que será presentada por el adaptador.
     */
    private List<TItem> mItems;

    /**
     * Inicializa la instancia con una colección vacía.
     *
     * @param layout Layout que representará cada elemento.
     */
    public AdapterBase(@LayoutRes int layout) {
        this(layout, new ArrayList<TItem>());
    }

    /**
     * Inicializa la instancia especificando la colección que será contenida en el
     * adaptador.
     *
     * @param layout Layout que representará cada elemento.
     * @param items  Colección de elementos.
     */
    public AdapterBase(@LayoutRes int layout, List<TItem> items) {
        mItems = new ArrayList<>(items);
        mLayoutID = layout;
    }

    /**
     * Agrega un elemento nuevo a la colección de datos.
     *
     * @param item Elemento a agregar.
     * @see AdapterBase#addAll(List)
     */
    public void add(TItem item) {
        Objects.requireNonNull(item, "El elemento no puede ser un valor null");
        mItems.add(item);

        hideEmptyView();

        notifyDataSetChanged();
    }

    /**
     * Agrega cada uno de los elementos de la colección.
     *
     * @param items Colección de datos a agregar.
     * @see AdapterBase#add(TItem)
     */
    public void addAll(List<TItem> items) {
        Objects.requireNonNull(items);
        if (items.size() == 0)
            return;

        mItems.addAll(items);

        hideEmptyView();

        notifyDataSetChanged();
    }

    /**
     * Vacía la colección del adaptador y visualiza el {@link AdapterBase#mEmptyView} de
     * ser posible para indicar al usuario que no hay datos que mostrar.
     */
    public void clear() {
        if (mItems.size() == 0)
            return;

        mItems.clear();

        showEmptyView();

        notifyDataSetChanged();
    }

    /**
     * Obtiene la vista que representa al adaptador cuando éste no contiene elementos.
     *
     * @return Vista que representa a la colección vacía.
     */
    public View getEmptyView() {
        return mEmptyView;
    }

    /**
     * Establece la vista que se mostrará cuando la colección está vacía. Este {@link View}
     * deberá estar incluido en el layout de la vista donde se visualizará, ya que el
     * adaptador solo controla la propiedad {@link View#setVisibility(int)} estableciendo
     * los valores {@link View#GONE} para ocultar el elemento o {@link View#VISIBLE} para
     * visualizarlo.
     *
     * @param emptyView Vista que representa a la colección vacía.
     */
    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
    }

    /**
     * Obtiene la cantidad total de elementos que se muestran en el adaptador.
     *
     * @return Cantidad de elementos.
     */
    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * Obtiene la colección que es gestionado por el adaptador.
     *
     * @return Colección de elementos.
     */
    protected List<TItem> getItems() {
        return mItems;
    }

    /**
     * Oculta la vista cuando la colección contiene elementos que mostrar.
     */
    protected void hideEmptyView() {

        if (mEmptyView == null)
            return;

        mEmptyView.setVisibility(View.GONE);
    }

    /**
     * Vincula el {@link RecyclerView.ViewHolder} a los elementos de la colección.
     *
     * @param viewHolder ViewHolder a vincular.
     * @param position   Posición en la colección del elemento a vincular.
     */
    @Override
    public abstract void onBindViewHolder(@NonNull TViewHolder viewHolder, int position);

    /**
     * Crea un {@link RecyclerView.ViewHolder} que representará a un elemento de la
     * colección.
     *
     * @param group    Contenedor de los {@link RecyclerView.ViewHolder}.
     * @param position Posición del elemento a representar.
     * @return Nueva instacia de {@link RecyclerView.ViewHolder}.
     */
    @Override
    @NonNull
    public TViewHolder onCreateViewHolder(@NonNull ViewGroup group, int position) {
        View view = LayoutInflater.from(group.getContext())
                .inflate(mLayoutID, group, false);

        return createViewHolder(view);
    }

    /**
     * Crea una instancia de la implementación de {@link RecyclerView.ViewHolder}.
     *
     * @param view Vista que contiene el layout del elemento a representar.
     * @return Una instancia de {@link TViewHolder}.
     */
    protected abstract TViewHolder createViewHolder(View view);

    /**
     * Remueve el elemento especificado de la colección del adaptador.
     *
     * @param item Elemento a borrar.
     */
    public boolean remove(TItem item) {
        Objects.requireNonNull(item);

        if (!mItems.contains(item))
            return false;

        boolean res = mItems.remove(item);

        notifyDataSetChanged();

        return res;
    }

    /**
     * Remueve cada uno de los elementos contenidos en la colección del adaptador.
     *
     * @param items Colección de elementos a remover.
     */
    public boolean removeAll(List<TItem> items) {
        boolean res = false;

        for (TItem item : items)
            res |= mItems.remove(item);

        notifyDataSetChanged();

        return res;
    }

    /**
     * Muestra la vista que representa a la colección vacía.
     */
    protected void showEmptyView() {

        if (mEmptyView == null)
            return;

        mEmptyView.setVisibility(View.VISIBLE);
    }
}//end AdapterBase